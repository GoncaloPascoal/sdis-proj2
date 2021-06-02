package workers;

import chord.ChordNode;
import chord.ChordNodeInfo;
import chord.ChordTask;
import jsse.ClientThread;
import messages.FindSuccessorMessage;
import messages.PutChunkMessage;
import messages.RemovedMessage;
import protocol.ChunkIdentifier;
import protocol.Peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoveChunkThread extends Thread {
    private final ChunkIdentifier identifier;
    private final InetSocketAddress initiatorAddress;

    public RemoveChunkThread(ChunkIdentifier identifier, InetSocketAddress initiatorAddress) {
        this.identifier = identifier;
        this.initiatorAddress = initiatorAddress;
    }

    @Override
    public void run() {
        long key;

        try {
            key = ChordNode.generateKey((identifier.fileId + "_" + identifier.chunkNumber).getBytes());
        }
        catch (NoSuchAlgorithmException ex) {
            System.err.println(ex.getMessage());
            return;
        }

        ChordNode chordNode = Peer.state.chordNode;

        String path = "peer" + Peer.id + File.separator + identifier.fileId + File.separator + identifier.chunkNumber;
        File chunkFile = new File(path);

        int chunkSize;
        byte[] chunkData = new byte[Peer.CHUNK_MAX_SIZE];

        if (chunkFile.length() > 0) {
            try {
                boolean readSuccessfully = true;
                FileInputStream stream = new FileInputStream(chunkFile);

                if ((chunkSize = stream.read(chunkData)) <= 0) {
                    System.err.println("Error when reading from chunk file " + identifier.chunkNumber + " of file " +
                            identifier.fileId);
                    readSuccessfully = false;
                }

                stream.close();
                if (readSuccessfully) {
                    byte[] body = new byte[chunkSize];
                    System.arraycopy(chunkData, 0, body, 0, chunkSize);

                    PutChunkMessage putChunkMessage = new PutChunkMessage(Peer.version, Peer.id, identifier.fileId,
                            identifier.chunkNumber, 1, initiatorAddress, body);

                    FindSuccessorMessage findSuccessorMessage = new FindSuccessorMessage(Peer.version, Peer.id, key, Peer.address);
                    chordNode.tasksMap.putIfAbsent(key, new ConcurrentLinkedQueue<>());
                    chordNode.tasksMap.get(key).add(new ChordTask() {
                        @Override
                        public void performTask(ChordNodeInfo nodeInfo) {
                            try {
                                ClientThread thread = new ClientThread(nodeInfo.address, putChunkMessage);
                                Peer.executor.execute(thread);
                            }
                            catch (IOException | GeneralSecurityException ex) {
                                System.err.println("Error when sending PUT_CHUNK message: " + ex.getMessage());
                            }
                        }
                    });

                    ClientThread thread = new ClientThread(chordNode.getSuccessorInfo().address, findSuccessorMessage);
                    Peer.executor.execute(thread);
                }
            }
            catch (IOException | GeneralSecurityException ex) {
                System.err.println("Error when initiating PUT_CHUNK protocol after reclaim: " + ex.getMessage());
                return;
            }

            if (!chunkFile.delete()) {
                System.err.println("Couldn't delete chunk " + identifier.chunkNumber + " of file with ID " + identifier.fileId);
                return;
            }
        }

        File folder = chunkFile.getParentFile();

        if (folder != null) {
            String[] fileNames = folder.list();

            // If the parent folder is empty, delete it as well
            if (fileNames != null && fileNames.length == 0) {
                if (!folder.delete()) {
                    System.err.println("Couldn't delete folder of file with ID " + identifier.fileId);
                }
            }
        }

        try {
            RemovedMessage message = new RemovedMessage(Peer.version, Peer.id, identifier.fileId, identifier.chunkNumber, Peer.address);
            ClientThread thread = new ClientThread(initiatorAddress, message);
            Peer.executor.execute(thread);
        }
        catch (IOException | GeneralSecurityException ex) {
            System.err.println("Error when sending REMOVED message: " + ex.getMessage());
        }
    }
}
