package protocol;

import java.io.Serializable;
import java.util.Objects;

public class ChunkIdentifier implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String fileId;
    public final int chunkNumber;

    public ChunkIdentifier(String fileId, int chunkNumber) {
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkIdentifier)) return false;

        ChunkIdentifier that = (ChunkIdentifier) o;
        return chunkNumber == that.chunkNumber && Objects.equals(fileId, that.fileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, chunkNumber);
    }
}
