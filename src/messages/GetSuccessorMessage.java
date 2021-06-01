package messages;

import java.net.InetSocketAddress;

public class GetSuccessorMessage extends Message {
    public static final String name = "GET_SUCCESSOR";

    public InetSocketAddress initiatorAddress;

    public GetSuccessorMessage(String protocolVersion, int peerId, InetSocketAddress initiatorAddress) {
        super(protocolVersion, peerId);
        this.initiatorAddress = initiatorAddress;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId),
                initiatorAddress.getAddress().getHostAddress(), String.valueOf(initiatorAddress.getPort()) };

        return String.join(" ", components);
    }

    public static GetSuccessorMessage parse(String header) {
        // <Version> GET_SUCCESSOR <SenderId> <InitiatorHostname> <InitiatorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 5 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);

        String initiatorHostname = headerComponents[3];
        int initiatorPort = Integer.parseInt(headerComponents[4]);

        return new GetSuccessorMessage(protocolVersion, senderId, new InetSocketAddress(initiatorHostname, initiatorPort));
    }
}
