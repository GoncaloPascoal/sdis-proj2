package messages;

import chord.ChordNodeInfo;

import java.net.InetSocketAddress;

public class PredecessorMessage extends Message {
    public static final String name = "PREDECESSOR";

    public final ChordNodeInfo predecessorInfo;

    public PredecessorMessage(String protocolVersion, int peerId, ChordNodeInfo predecessorInfo) {
        super(protocolVersion, peerId);
        this.predecessorInfo = predecessorInfo;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), String.valueOf(predecessorInfo.id),
                predecessorInfo.address.getAddress().getHostAddress(), String.valueOf(predecessorInfo.address.getPort()) };

        return String.join(" ", components);
    }

    public static PredecessorMessage parse(String header, byte[] body) {
        // <Version> PREDECESSOR <SenderId> <PredecessorKey> <PredecessorHostname> <PredecessorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 6 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);

        long predecessorKey = Integer.parseInt(headerComponents[3]);
        String predecessorHostname = headerComponents[4];
        int predecessorPort = Integer.parseInt(headerComponents[5]);

        InetSocketAddress predecessorAddress = new InetSocketAddress(predecessorHostname, predecessorPort);

        return new PredecessorMessage(protocolVersion, senderId, new ChordNodeInfo(predecessorKey, predecessorAddress));
    }
}
