package messages;

import protocol.Peer;

public abstract class Message {
    public static final String headerSeparator = "\r\n\r\n";
    public static final int MAX_SIZE = Peer.CHUNK_MAX_SIZE + 1000;

    public final String protocolVersion;
    public int senderId;
    public byte[] body;

    public Message(String protocolVersion, int senderId, byte[] body) {
        this.protocolVersion = protocolVersion;
        this.senderId = senderId;
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