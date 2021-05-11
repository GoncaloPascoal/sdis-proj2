package messages;

import java.net.InetSocketAddress;
import java.util.List;

import utils.Utils;

public class PutChunkMessage extends Message {
    public final String fileId;
    public final int chunkNumber;
    public int replicationDegree;
    public InetSocketAddress initiatorAddress;

    public PutChunkMessage(String protocolVersion, int peerId, String fileId, int chunkNumber, int replicationDegree,
                           InetSocketAddress initiatorAddress, byte[] body) {
        super(protocolVersion, peerId, body);

        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.replicationDegree = replicationDegree;
        this.initiatorAddress = initiatorAddress;
    }

    @Override
    public String buildHeader() {
        return protocolVersion + " " + peerId + " " + fileId + " " + chunkNumber + " " + replicationDegree + " " +
                initiatorAddress.getHostName() + " " + initiatorAddress.getPort();
    }

    public static PutChunkMessage parse(byte[] bytes) {
        // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <InitiatorHostname> <InitiatorPort> <CRLF><CRLF><Body>
        List<byte[]> components = Utils.splitMessage(bytes);

        if (components.size() != 2) {
            return null;
        }

        String header = new String(components.get(0));
        byte[] body = components.get(1);

        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 8) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int peerId = Integer.parseInt(headerComponents[2]);
        String fileId = headerComponents[3];
        int chunkNumber = Integer.parseInt(headerComponents[4]),
                replicationDegree = Integer.parseInt(headerComponents[5]);

        String initiatorHostname = headerComponents[6];
        int initiatorPort = Integer.parseInt(headerComponents[7]);

        InetSocketAddress initiatorAddress = new InetSocketAddress(initiatorHostname, initiatorPort);

        return new PutChunkMessage(protocolVersion, peerId, fileId, chunkNumber, replicationDegree, initiatorAddress, body);
    }
}
