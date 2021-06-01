package protocol;

import jsse.ClientThread;
import messages.VerifyChunkMessage;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class VerifyChunksThread extends Thread {
    @Override
    public void run() {
        Random random = new Random();

        try {
            for (Map.Entry<ChunkIdentifier, Set<InetSocketAddress>> entry : Peer.state.chunkReplicationDegreeMap.entrySet()) {
                Set<InetSocketAddress> peers = entry.getValue();
                if (!entry.getValue().isEmpty()) {
                    Optional<InetSocketAddress> optional = peers.stream().skip(random.nextInt(peers.size())).findFirst();

                    if (optional.isPresent()) {
                        InetSocketAddress randomAddress = optional.get();
                        ChunkIdentifier key = entry.getKey();

                        VerifyChunkMessage message = new VerifyChunkMessage(Peer.version, Peer.id, key.fileId, key.chunkNumber, Peer.address);
                        ClientThread thread = new ClientThread(randomAddress, message);
                        Peer.executor.execute(thread);
                    }
                }
            }
        }
        catch (Exception ex) {
            System.err.println("Exception during VerifyChunksThread: " + ex.getMessage());
        }
    }
}
