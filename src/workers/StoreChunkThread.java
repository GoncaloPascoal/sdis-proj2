package workers;

import chord.ChordNode;
import jsse.ClientThread;
import messages.PutChunkMessage;
import messages.StoredMessage;
import protocol.ChunkIdentifier;
import protocol.ChunkInformation;
import protocol.Peer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class StoreChunkThread extends Thread {
    public static final Object lock = new Object();

    private final PutChunkMessage message;

    public StoreChunkThread(PutChunkMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        ChunkIdentifier identifier = new ChunkIdentifier(message.fileId, message.chunkNumber);
        boolean stored = false;

        // Attempt to store chunk
        synchronized (lock) {
            if ((Peer.state.maxDiskSpace == null || Peer.state.getSpaceOccupied() + message.body.length <= Peer.state.maxDiskSpace)
                && !Peer.state.storedChunksMap.containsKey(identifier)) {
                // Have enough space to store this chunk and the chunk isn't already stored
                stored = true;
                Peer.state.storedChunksMap.put(identifier,
                        new ChunkInformation(message.body.length, message.initiatorAddress));
            }
        }

        if (stored) {
            // Store chunk in file system
            String path = "peer" + Peer.id + File.separator + message.fileId + File.separator + message.chunkNumber;
            File chunkFile = new File(path);

            try {
                if (!chunkFile.exists()) {
                    chunkFile.getParentFile().mkdirs();
                    chunkFile.createNewFile();

                    FileOutputStream stream = new FileOutputStream(chunkFile);
                    stream.write(message.body);
                    stream.close();
                }
            }
            catch (IOException ex) {
                System.err.println("IO exception when storing chunk: " + ex.getMessage());
                synchronized (lock) {
                    Peer.state.storedChunksMap.remove(identifier);
                }
                message.forwardToSuccessor(false);
                return;
            }

            // Send STORED message
            StoredMessage storedMessage = new StoredMessage(Peer.version, Peer.id, message.fileId, message.chunkNumber, Peer.address);
            try {
                ClientThread storedThread = new ClientThread(message.initiatorAddress, storedMessage);
                Peer.executor.execute(storedThread);
            }
            catch (IOException | GeneralSecurityException ex) {
                System.err.println("Error when sending STORED message: " + ex.getMessage());
            }
        }

        message.forwardToSuccessor(stored);
    }
}
