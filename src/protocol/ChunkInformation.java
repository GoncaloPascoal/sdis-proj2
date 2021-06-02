package protocol;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class ChunkInformation implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int size;
    public final InetSocketAddress initiatorAddress;

    public ChunkInformation(int size, InetSocketAddress initiatorAddress) {
        this.size = size;
        this.initiatorAddress = initiatorAddress;
    }
}
