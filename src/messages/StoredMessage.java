package messages;

public class StoredMessage extends Message {
    public static final String name = "STORED";

    public final String fileId;
    public final int chunkNumber;

    public StoredMessage(String protocolVersion, int senderId, String fileId, int chunkNumber) {
        super(protocolVersion, senderId);

        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), fileId, String.valueOf(chunkNumber) };

        return String.join(" ", components);
    }

    public static StoredMessage parse(String header) {
        // <Version> STORED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 5 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);
        String fileId = headerComponents[3];
        int chunkNumber = Integer.parseInt(headerComponents[4]);

        return new StoredMessage(protocolVersion, senderId, fileId, chunkNumber);
    }
}
