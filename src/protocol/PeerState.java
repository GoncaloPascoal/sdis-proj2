package protocol;

import chord.ChordNode;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PeerState implements Serializable {
    public ChordNode chordNode;

    public Long maxDiskSpace = null;

    public long getSpaceOccupied() {
        return storedChunksMap.values().stream().mapToInt((chunk -> chunk.size)).sum();
    }

    // For each file ID + chunk number, this hash map stores a set with the addresses and ports of all peers who have
    // backed up that chunk. The actual replication degree of the chunk, therefore, is the size of the set.
    public ConcurrentHashMap<ChunkIdentifier, Set<InetSocketAddress>> chunkReplicationDegreeMap = new ConcurrentHashMap<>();

    // Hash map containing information about the files that this peer has initiated the backup of
    public ConcurrentHashMap<String, FileInformation> backupFilesMap = new ConcurrentHashMap<>();

    // Hash map containing information about chunks this peer is backing up
    public ConcurrentHashMap<ChunkIdentifier, ChunkInformation> storedChunksMap = new ConcurrentHashMap<>();
}
