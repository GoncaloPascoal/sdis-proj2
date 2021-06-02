package messages;

public class AliveMessage extends Message {
    public static final String name = "ALIVE";

    public AliveMessage(String protocolVersion, int peerId) {
        super(protocolVersion, peerId);
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId) };
        return String.join(" ", components);
    }

    public static AliveMessage parse(String header) {
        // <Version> ALIVE <SenderId> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 3 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);

        return new AliveMessage(protocolVersion, senderId);
    }
}
