package workers;

import jsse.ClientThread;
import messages.RemovedMessage;
import protocol.ChunkIdentifier;
import protocol.Peer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;

public class RemoveChunkThread extends Thread {
    private final ChunkIdentifier identifier;
    private final InetSocketAddress initiatorAddress;

    public RemoveChunkThread(ChunkIdentifier identifier, InetSocketAddress initiatorAddress) {
        this.identifier = identifier;
        this.initiatorAddress = initiatorAddress;
    }

    @Override
    public void run() {
        String path = "peer" + Peer.id + File.separator + identifier.fileId + File.separator + identifier.chunkNumber;
        File chunkFile = new File(path);

        if (chunkFile.exists()) {
            if (!chunkFile.delete()) {
                System.err.println("Couldn't delete chunk " + identifier.chunkNumber + " of file with ID " + identifier.fileId);
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
            RemovedMessage message = new RemovedMessage(Peer.version, Peer.id, identifier.fileId, identifier.chunkNumber);
            ClientThread thread = new ClientThread(initiatorAddress, message);
            Peer.executor.execute(thread);
        }
        catch (IOException | GeneralSecurityException ex) {
            System.err.println("Error when sending REMOVED message: " + ex.getMessage());
        }
    }
}
