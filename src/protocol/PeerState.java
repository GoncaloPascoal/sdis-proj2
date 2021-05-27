package protocol;

import chord.ChordNode;

import java.io.Serializable;

public class PeerState implements Serializable {
    public ChordNode chordNode;

    public Long maxDiskSpace = null;

    public long getSpaceOccupied() {
        return 0; //storedChunksMap.values().stream().mapToInt(chunk -> chunk.size).sum();
    }
}
