package messages;

import chord.ChordNodeInfo;

import java.net.InetSocketAddress;

public class NodeSuccessorMessage extends Message {
    public static final String name = "NODE_SUCCESSOR";

    public final ChordNodeInfo successorInfo;

    public NodeSuccessorMessage(String protocolVersion, int peerId, ChordNodeInfo successorInfo) {
        super(protocolVersion, peerId);
        this.successorInfo = successorInfo;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId),
                    String.valueOf(successorInfo.id), successorInfo.address.getAddress().getHostAddress(),
                    String.valueOf(successorInfo.address.getPort()) };

        return String.join(" ", components);
    }

    public static NodeSuccessorMessage parse(String header) {
        // <Version> NODE_SUCCESSOR <SenderId> <SuccessorKey> <SuccessorHostname> <SuccessorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 6 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);

        long successorKey = Integer.parseInt(headerComponents[3]);
        String successorHostname = headerComponents[4];
        int successorPort = Integer.parseInt(headerComponents[5]);

        InetSocketAddress successorAddress = new InetSocketAddress(successorHostname, successorPort);

        return new NodeSuccessorMessage(protocolVersion, senderId, new ChordNodeInfo(successorKey, successorAddress));
    }
}
