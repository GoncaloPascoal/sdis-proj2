package messages;

import java.net.InetSocketAddress;

public class StartPutChunkMessage extends Message {
    public static final String name = "START_PUT_CHUNK";

    public final String fileId;
    public final int chunkNumber;
    public int replicationDegree;
    public InetSocketAddress initiatorAddress;

    public StartPutChunkMessage(String protocolVersion, int senderId, String fileId, int chunkNumber, int replicationDegree,
                           InetSocketAddress initiatorAddress) {
        super(protocolVersion, senderId);

        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.replicationDegree = replicationDegree;
        this.initiatorAddress = initiatorAddress;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), fileId, String.valueOf(chunkNumber),
                String.valueOf(replicationDegree), initiatorAddress.getHostName(), String.valueOf(initiatorAddress.getPort()) };

        return String.join(" ", components);
    }

    public static StartPutChunkMessage parse(String header) {
        // <Version> START_PUT_CHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <InitiatorHostname> <InitiatorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 8 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);
        String fileId = headerComponents[3];
        int chunkNumber = Integer.parseInt(headerComponents[4]),
                replicationDegree = Integer.parseInt(headerComponents[5]);

        String initiatorHostname = headerComponents[6];
        int initiatorPort = Integer.parseInt(headerComponents[7]);

        InetSocketAddress initiatorAddress = new InetSocketAddress(initiatorHostname, initiatorPort);

        return new StartPutChunkMessage(protocolVersion, senderId, fileId, chunkNumber, replicationDegree, initiatorAddress);
    }
}
