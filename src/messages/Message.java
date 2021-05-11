package messages;

public abstract class Message {
    public static final String headerSeparator = "\r\n\r\n";

    public final String protocolVersion;
    public final int peerId;
    public byte[] body;

    public Message(String protocolVersion, int peerId, byte[] body) {
        this.protocolVersion = protocolVersion;
        this.peerId = peerId;
        this.body = body;
    }

    public Message(String protocolVersion, int peerId) {
        this(protocolVersion, peerId, new byte[0]);
    }

    public abstract String buildHeader();

    public byte[] build() {
        byte[] headerBytes = (buildHeader() + headerSeparator).getBytes();
        byte[] bytes = new byte[headerBytes.length + body.length];

        System.arraycopy(headerBytes, 0, bytes, 0, headerBytes.length);
        System.arraycopy(body, 0, bytes, headerBytes.length, body.length);

        return bytes;
    }
}