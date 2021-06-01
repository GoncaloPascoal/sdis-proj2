package messages;

import chord.ChordNode;
import jsse.ClientThread;
import protocol.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

public class PutChunkMessage extends Message {
    public static final String name = "PUT_CHUNK";

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
        // <Version> PUT_CHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <InitiatorHostname> <InitiatorPort> <CRLF><CRLF><Body>
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

        return new PutChunkMessage(protocolVersion, senderId, fileId, chunkNumber, replicationDegree, initiatorAddress, body);
    }

    public void forwardToSuccessor(boolean stored) {
        // Calculate the chunk's key
        long key;
        try {
            key = ChordNode.generateKey((fileId + "_" + chunkNumber).getBytes());
        }
        catch (NoSuchAlgorithmException ex) {
            System.err.println(ex.getMessage());
            return;
        }

        ChordNode chordNode = Peer.state.chordNode;

        if (ChordNode.isKeyBetween(key, chordNode.selfInfo.id, chordNode.getSuccessorInfo().id)) {
            // To prevent a PUT_CHUNK message from passing through the chord ring more than once, if
            // the message key is between this node's id and its successor's id, the message is not forwarded
            return;
        }

        // Forward message to successor
        senderId = Peer.id;
        if (stored) replicationDegree -= 1;
        if (replicationDegree != 0) {
            try {
                ClientThread thread = new ClientThread(chordNode.getSuccessorInfo().address, this);
                Peer.executor.execute(thread);
            }
            catch (IOException | GeneralSecurityException ex) {
                System.err.println("Error when forwarding PUT_CHUNK message: " + ex.getMessage());
            }
        }
    }
}
