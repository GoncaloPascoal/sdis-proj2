package messages;

import java.net.InetSocketAddress;

public class SuccessorMessage extends Message {
    public static final String name = "SUCCESSOR";

    public final int key;
    public final InetSocketAddress successorAddress;

    public SuccessorMessage(String protocolVersion, int peerId, int key, InetSocketAddress successorAddress) {
        super(protocolVersion, peerId);

        this.key = key;
        this.successorAddress = successorAddress;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), String.valueOf(key),
                successorAddress.getHostName(), String.valueOf(successorAddress.getPort()) };

        return String.join(" ", components);
    }

    public static SuccessorMessage parse(String header, byte[] body) {
        // <Version> SUCCESSOR <SenderId> <Key> <SuccessorHostname> <SuccessorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 6) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]),
                key = Integer.parseInt(headerComponents[3]);

        String successorHostname = headerComponents[4];
        int successorPort = Integer.parseInt(headerComponents[5]);

        InetSocketAddress successorAddress = new InetSocketAddress(successorHostname, successorPort);

        return new SuccessorMessage(protocolVersion, senderId, key, successorAddress);
    }
}
