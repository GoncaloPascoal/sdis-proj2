package protocol;

import java.io.Serializable;

public class FileInformation implements Serializable {
    private static final long serialVersionUID = 1L;

    public String fileId;
    public int desiredReplicationDegree;
    public int numChunks;

    public FileInformation(String fileId, int desiredReplicationDegree, int numChunks) {
        this.fileId = fileId;
        this.desiredReplicationDegree = desiredReplicationDegree;
        this.numChunks = numChunks;
    }
}
