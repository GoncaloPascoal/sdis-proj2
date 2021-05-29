package protocol;

import java.io.Serializable;

public class ChunkInformation implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int size, desiredReplicationDegree;

    public ChunkInformation(int size, int desiredReplicationDegree) {
        this.size = size;
        this.desiredReplicationDegree = desiredReplicationDegree;
    }
}
