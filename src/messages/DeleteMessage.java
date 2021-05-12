package messages;

public class DeleteMessage extends Message {
    public static final String name = "DELETE";

    public final String fileId;

    public DeleteMessage(String protocolVersion, int senderId, String fileId, byte[] body) {
        super(protocolVersion, senderId, body);

        this.fileId = fileId;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), fileId };

        return String.join(" ", components);
    }

    public static DeleteMessage parse(String header, byte[] body) {
        // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 4 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);
        String fileId = headerComponents[3];

        return new DeleteMessage(protocolVersion, senderId, fileId, body);
    }
}
