package messages;

import chord.ChordNodeInfo;

import java.net.InetSocketAddress;

public class SuccessorMessage extends Message {
    public static final String name = "SUCCESSOR";

    public final long key;
    public final ChordNodeInfo nodeInfo;

    public SuccessorMessage(String protocolVersion, int peerId, long key, ChordNodeInfo nodeInfo) {
        super(protocolVersion, peerId);

        this.key = key;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), String.valueOf(key),
                String.valueOf(nodeInfo.id), nodeInfo.address.getAddress().getHostAddress(),
                String.valueOf(nodeInfo.address.getPort()) };

        return String.join(" ", components);
    }

    public static SuccessorMessage parse(String header, byte[] body) {
        // <Version> SUCCESSOR <SenderId> <Key> <SuccessorKey> <SuccessorHostname> <SuccessorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 7 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);
        long key = Long.parseLong(headerComponents[3]);

        long successorKey = Integer.parseInt(headerComponents[4]);
        String successorHostname = headerComponents[5];
        int successorPort = Integer.parseInt(headerComponents[6]);

        InetSocketAddress successorAddress = new InetSocketAddress(successorHostname, successorPort);

        return new SuccessorMessage(protocolVersion, senderId, key, new ChordNodeInfo(successorKey, successorAddress));
    }
}
