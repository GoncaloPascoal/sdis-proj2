package workers;

import messages.ChunkMessage;
import protocol.Peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RestoreChunkThread extends Thread {
    private final ChunkMessage message;

    public RestoreChunkThread(ChunkMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        int offset = Peer.CHUNK_MAX_SIZE * message.chunkNumber;
        AsynchronousFileChannel channel = Peer.restoredFileChannelMap.get(message.fileId);

        ByteBuffer buffer = ByteBuffer.wrap(message.body);

        /* Write contents of the chunk starting at the specified offset (if the file isn't large enough yet, it will
         * be padded with unspecified data: this is done by the write method of AsynchronousFileChannel) */
        Future<Integer> future = channel.write(buffer, offset);

        try {
            Integer bytesWritten = future.get();
            System.out.println("Wrote " + bytesWritten + " bytes at position " + offset);
            Peer.chunksToRestoreMap.get(message.fileId).remove(message.chunkNumber);

            if (Peer.chunksToRestoreMap.get(message.fileId).isEmpty()) {
                // All RestoreChunk threads have finished writing, can close the channel
                channel.close();
                Peer.chunksToRestoreMap.remove(message.fileId);
            }
        }
        catch (InterruptedException | ExecutionException | IOException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
