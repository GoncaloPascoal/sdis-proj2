package messages;

import java.net.InetSocketAddress;

public class FindSuccessorMessage extends Message {
    public static final String name = "FIND_SUCCESSOR";

    public final long key;
    public final InetSocketAddress initiatorAddress;

    public FindSuccessorMessage(String protocolVersion, int peerId, long key, InetSocketAddress initiatorAddress) {
        super(protocolVersion, peerId);

        this.key = key;
        this.initiatorAddress = initiatorAddress;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), String.valueOf(key),
                initiatorAddress.getAddress().getHostAddress(), String.valueOf(initiatorAddress.getPort()) };

        return String.join(" ", components);
    }

    public static FindSuccessorMessage parse(String header, byte[] body) {
        // <Version> FIND_SUCCESSOR <SenderId> <Key> <InitiatorHostname> <InitiatorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 6 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);
        long key = Long.parseLong(headerComponents[3]);

        String initiatorHostname = headerComponents[4];
        int initiatorPort = Integer.parseInt(headerComponents[5]);

        InetSocketAddress initiatorAddress = new InetSocketAddress(initiatorHostname, initiatorPort);

        return new FindSuccessorMessage(protocolVersion, senderId, key, initiatorAddress);
    }
}
