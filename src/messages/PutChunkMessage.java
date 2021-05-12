package messages;

import java.net.InetSocketAddress;

public class PutChunkMessage extends Message {
    public static final String name = "PUTCHUNK";

    public final String fileId;
    public final int chunkNumber;
    public int replicationDegree;
    public InetSocketAddress initiatorAddress;

    public PutChunkMessage(String protocolVersion, int senderId, String fileId, int chunkNumber, int replicationDegree,
                           InetSocketAddress initiatorAddress, byte[] body) {
        super(protocolVersion, senderId, body);

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

    public static PutChunkMessage parse(String header, byte[] body) {
        // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <InitiatorHostname> <InitiatorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 8) {
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

        return new PutChunkMessage(protocolVersion, senderId, fileId, chunkNumber, replicationDegree, initiatorAddress, body);
    }
}
