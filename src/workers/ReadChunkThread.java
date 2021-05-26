package workers;

import chord.ChordNode;
import chord.ChordNodeInfo;
import chord.ChordTask;
import jsse.ClientThread;
import messages.FindSuccessorMessage;
import messages.PutChunkMessage;
import protocol.Peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ReadChunkThread extends Thread {
    private final AsynchronousFileChannel channel;
    private final String fileId;
    private final int chunkNumber, replicationDegree;

    public ReadChunkThread(AsynchronousFileChannel channel, String fileId, int chunkNumber, int replicationDegree) {
        this.channel = channel;
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.replicationDegree = replicationDegree;
    }

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(Peer.CHUNK_MAX_SIZE);
        Future<Integer> future = channel.read(buffer, chunkNumber * Peer.CHUNK_MAX_SIZE);

        try {
            Integer bytesRead = future.get();

            if (bytesRead == -1) {
                bytesRead = 0;
            }

            if (!buffer.hasArray()) {
                System.err.println("Error: ByteBuffer in ReadChunkThread has no backing array");
                return;
            }

            Peer.chunksToReadMap.get(fileId).remove(chunkNumber);
            if (Peer.chunksToReadMap.get(fileId).isEmpty()) {
                // All ReadChunk threads have finished writing, can close the channel
                channel.close();
                Peer.chunksToReadMap.remove(fileId);
            }

            ChordNode chordNode = Peer.state.chordNode;

            // Calculate chord key for chunk
            long key = ChordNode.generateKey((fileId + "_" + chunkNumber).getBytes());

            PutChunkMessage putChunkMessage = new PutChunkMessage(Peer.version, Peer.id, fileId, chunkNumber,
                    replicationDegree, Peer.address, buffer.array());

            // key in (self.id, successor.id] -> successor is responsible for the key
            if (ChordNode.isKeyBetween(key, chordNode.selfInfo.id, chordNode.getSuccessorInfo().id, false, true)) {
                ClientThread putChunkThread = new ClientThread(chordNode.getSuccessorInfo().address, putChunkMessage);
                Peer.executor.execute(putChunkThread);
            }
            else {
                FindSuccessorMessage findSuccessorMessage = new FindSuccessorMessage(Peer.version, Peer.id, key, Peer.address);
                ChordNodeInfo closestPrecedingNode = chordNode.getClosestPrecedingNode(key);

                chordNode.tasksMap.putIfAbsent(key, new ConcurrentLinkedQueue<>());
                chordNode.tasksMap.get(key).add(new ChordTask() {
                    @Override
                    public void performTask(ChordNodeInfo successorInfo) {
                        try {
                            ClientThread putChunkThread = new ClientThread(successorInfo.address, putChunkMessage);
                            Peer.executor.execute(putChunkThread);
                        }
                        catch (Exception ex) {
                            System.err.println("Exception when attempting to sent PUT_CHUNK message: " + ex.getMessage());
                        }
                    }
                });

                ClientThread thread = new ClientThread(closestPrecedingNode.address, findSuccessorMessage);
                Peer.executor.execute(thread);
            }
        }
        catch (Exception ex) {
            System.err.println("Exception in ReadChunkThread: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
