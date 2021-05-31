package protocol;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class ChunkInformation implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int size, desiredReplicationDegree;
    public final InetSocketAddress initiatorAddress;

    public ChunkInformation(int size, int desiredReplicationDegree, InetSocketAddress initiatorAddress) {
        this.size = size;
        this.desiredReplicationDegree = desiredReplicationDegree;
        this.initiatorAddress = initiatorAddress;
    }
}
