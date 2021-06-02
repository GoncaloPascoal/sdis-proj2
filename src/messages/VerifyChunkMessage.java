package messages;

import java.net.InetSocketAddress;

public class VerifyChunkMessage extends Message {
    public static final String name = "VERIFY_CHUNK";

    public final String fileId;
    public final int chunkNumber;
    public final InetSocketAddress initiatorAddress;

    public VerifyChunkMessage(String protocolVersion, int senderId, String fileId, int chunkNumber, InetSocketAddress initiatorAddress) {
        super(protocolVersion, senderId);

        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.initiatorAddress = initiatorAddress;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), fileId, String.valueOf(chunkNumber),
                initiatorAddress.getHostName(), String.valueOf(initiatorAddress.getPort()) };

        return String.join(" ", components);
    }

    public static VerifyChunkMessage parse(String header) {
        // <Version> VERIFY_CHUNK <SenderId> <FileId> <ChunkNo> <InitiatorHostname> <InitiatorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 7 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);
        String fileId = headerComponents[3];
        int chunkNumber = Integer.parseInt(headerComponents[4]);

        String initiatorHostname = headerComponents[5];
        int initiatorPort = Integer.parseInt(headerComponents[6]);

        InetSocketAddress initiatorAddress = new InetSocketAddress(initiatorHostname, initiatorPort);

        return new VerifyChunkMessage(protocolVersion, senderId, fileId, chunkNumber, initiatorAddress);
    }
}
