package messages;

import chord.ChordNodeInfo;

import java.net.InetSocketAddress;

public class GetPredecessorMessage extends Message {
    public static final String name = "GET_PREDECESSOR";

    public ChordNodeInfo nodeInfo;

    public GetPredecessorMessage(String protocolVersion, int peerId, ChordNodeInfo nodeInfo) {
        super(protocolVersion, peerId);
        this.nodeInfo = nodeInfo;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), String.valueOf(nodeInfo.id),
                nodeInfo.address.getAddress().getHostAddress(), String.valueOf(nodeInfo.address.getPort()) };

        return String.join(" ", components);
    }

    public static GetPredecessorMessage parse(String header, byte[] body) {
        // <Version> GET_PREDECESSOR <SenderId> <InitiatorKey> <InitiatorHostname> <InitiatorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 6 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);

        long initiatorKey = Long.parseLong(headerComponents[3]);
        String initiatorHostname = headerComponents[4];
        int initiatorPort = Integer.parseInt(headerComponents[5]);

        InetSocketAddress initiatorAddress = new InetSocketAddress(initiatorHostname, initiatorPort);

        return new GetPredecessorMessage(protocolVersion, senderId, new ChordNodeInfo(initiatorKey, initiatorAddress));
    }
}
