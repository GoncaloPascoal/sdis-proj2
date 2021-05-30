package messages;

import java.net.InetSocketAddress;

public class RemovedMessage extends Message {
    public static final String name = "REMOVED";

    public final String fileId;
    public final int chunkNumber;
    public final InetSocketAddress senderAddress;

    public RemovedMessage(String protocolVersion, int senderId, String fileId, int chunkNumber, InetSocketAddress senderAddress) {
        super(protocolVersion, senderId);

        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.senderAddress = senderAddress;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), fileId, String.valueOf(chunkNumber),
                senderAddress.getAddress().getHostAddress(), String.valueOf(senderAddress.getPort()) };

        return String.join(" ", components);
    }

    public static RemovedMessage parse(String header) {
        // <Version> REMOVED <SenderId> <FileId> <ChunkNo> <SenderHostname> <SenderPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 7 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);
        String fileId = headerComponents[3];
        int chunkNumber = Integer.parseInt(headerComponents[4]);

        String senderHostname = headerComponents[5];
        int senderPort = Integer.parseInt(headerComponents[6]);

        InetSocketAddress senderAddress = new InetSocketAddress(senderHostname, senderPort);

        return new RemovedMessage(protocolVersion, senderId, fileId, chunkNumber, senderAddress);
    }
}
