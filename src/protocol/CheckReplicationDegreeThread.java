package protocol;

import jsse.ClientThread;
import messages.StartPutChunkMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Thread that runs periodically and verifies if the replication degree of a chunk has dropped below its desired
 * value. In that case, the peer asks one of the other peers that store the chunk to initiate a PUT_CHUNK protocol.
 * This is done because we cannot know if the initiator peer has a copy of the backed up file after the BACKUP protocol
 * is finished.
 */
public class CheckReplicationDegreeThread extends Thread {
    @Override
    public void run() {
        for (Map.Entry<ChunkIdentifier, Set<InetSocketAddress>> entry : Peer.state.chunkReplicationDegreeMap.entrySet()) {
            ChunkIdentifier key = entry.getKey();
            Set<InetSocketAddress> value = entry.getValue();

            int replicationDegreeDifference = Peer.state.desiredReplicationDegreeMap.get(key.fileId) - value.size();

            if (replicationDegreeDifference > 0) {
                // Replication degree has dropped below its desired value
                Optional<InetSocketAddress> first = value.stream().findFirst();

                if (first.isPresent()) {
                    // Ask other peer to initiate a PUT_CHUNK protocol
                    InetSocketAddress address = first.get();
                    StartPutChunkMessage message = new StartPutChunkMessage(Peer.version, Peer.id, key.fileId, key.chunkNumber,
                            replicationDegreeDifference, Peer.address);

                    try {
                        ClientThread thread = new ClientThread(address, message);
                        Peer.executor.execute(thread);
                    }
                    catch (IOException | GeneralSecurityException ex) {
                        System.out.println("Exception when sending START_PUT_CHUNK message: " + ex.getMessage());
                    }
                }
            }
        }
    }
}
