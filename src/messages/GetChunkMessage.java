package messages;

import chord.ChordNode;
import jsse.ClientThread;
import protocol.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

public class GetChunkMessage extends Message {
    public static final String name = "GET_CHUNK";

    public final String fileId;
    public final int chunkNumber;
    public final InetSocketAddress initiatorAddress;

    public GetChunkMessage(String protocolVersion, int senderId, String fileId, int chunkNumber, InetSocketAddress initiatorAddress) {
        super(protocolVersion, senderId);

        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.initiatorAddress = initiatorAddress;
    }

    @Override
    public String buildHeader() {
        String[] components = { protocolVersion, name, String.valueOf(senderId), fileId, String.valueOf(chunkNumber),
                initiatorAddress.getAddress().getHostAddress(), String.valueOf(initiatorAddress.getPort()) };

        return String.join(" ", components);
    }

    public static GetChunkMessage parse(String header) {
        // <Version> GET_CHUNK <SenderId> <FileId> <ChunkNo> <InitiatorHostname> <InitiatorPort> <CRLF><CRLF><Body>
        String[] headerComponents = header.split(" ");

        if (headerComponents.length != 7 || !headerComponents[1].equals(name)) {
            return null;
        }

        String protocolVersion = headerComponents[0];
        int senderId = Integer.parseInt(headerComponents[2]);
        String fileId = headerComponents[3];
        int chunkNumber = Integer.parseInt(headerComponents[4]);

        String initiatorHostname = headerComponents[5];
        int initiatorPort = Integer.parseInt(headerComponents[6]);

        InetSocketAddress initiatorAddress = new InetSocketAddress(initiatorHostname, initiatorPort);

        return new GetChunkMessage(protocolVersion, senderId, fileId, chunkNumber, initiatorAddress);
    }

    public void forwardToSuccessor() {
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
            // To prevent a GET_CHUNK message from passing through the chord ring more than once, if
            // the message key is between this node's id and its successor's id, the message is not forwarded
            return;
        }

        // Forward message to successor
        senderId = Peer.id;
        try {
            ClientThread thread = new ClientThread(chordNode.getSuccessorInfo().address, this);
            Peer.executor.execute(thread);
        }
        catch (IOException | GeneralSecurityException ex) {
            System.err.println("Error when forwarding PUT_CHUNK message: " + ex.getMessage());
        }
    }
}
