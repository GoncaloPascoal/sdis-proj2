package jsse;

import chord.ChordNode;
import chord.ChordNodeInfo;
import messages.FindSuccessorMessage;
import messages.GetChunkMessage;
import messages.Message;
import protocol.ChunkIdentifier;
import protocol.Peer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ClientThread extends SSLThread {
    private InetSocketAddress destinationAddress;
    private final Message message;

    public ClientThread(InetSocketAddress destinationAddress, Message message) throws GeneralSecurityException, IOException {
        super("TLS", Peer.keyStorePath, Peer.trustStorePath, Peer.password);

        this.destinationAddress = destinationAddress;
        this.message = message;
    }

    @Override
    public void run() {
        SSLEngine engine = context.createSSLEngine(destinationAddress.getAddress().getHostAddress(), destinationAddress.getPort());
        engine.setUseClientMode(true);

        try {
            SocketChannel socketChannel = SocketChannel.open();

            socketChannel.connect(destinationAddress);
            socketChannel.finishConnect();
            socketChannel.configureBlocking(false);

            doHandshake(socketChannel, engine);
            sendMessage(socketChannel, engine);
        }
        catch (IOException ex) {
            System.out.println("Exception in JSSE Client: " + ex.getMessage());

            ChordNode chordNode = Peer.state.chordNode;

            // We consider that the peer that has died no longer stores any chunks whose backup was initiated by this peer
            for (Map.Entry<ChunkIdentifier, Set<InetSocketAddress>> entry : Peer.state.chunkReplicationDegreeMap.entrySet()) {
                entry.getValue().remove(destinationAddress);
            }

            if (destinationAddress.equals(chordNode.predecessorInfo.address)) {
                // Predecessor has failed, set it to null
                chordNode.predecessorInfo = null;
            }

            if (destinationAddress.equals(chordNode.getSuccessorInfo().address)) {
                // The peer's successor appears to have died: replace all of its entries in the finger table with the next successor
                ChordNodeInfo successorInfo = chordNode.getSuccessorInfo(), newSuccessorInfo;

                if (!chordNode.successorDeque.isEmpty()) {
                    newSuccessorInfo = chordNode.successorDeque.pop();
                }
                else {
                    // No more successors, assume the chord node is alone
                    newSuccessorInfo = chordNode.selfInfo;
                }

                // Replace all occurrences of the successor in the finger table with the new successor
                for (int i = 0; i < ChordNode.keyBits; ++i) {
                    if (chordNode.fingerTable.get(i).equals(successorInfo)) {
                        chordNode.fingerTable.set(i, newSuccessorInfo);
                    }
                }
            }

            if (message instanceof GetChunkMessage) {
                // If a GetChunkMessage failed, attempt to contact next peer that has stored the requested chunk
                GetChunkMessage getChunkMessage = (GetChunkMessage) message;
                ChunkIdentifier identifier = new ChunkIdentifier(getChunkMessage.fileId, getChunkMessage.chunkNumber);
                Peer.state.chunkReplicationDegreeMap.get(identifier);

                Optional<InetSocketAddress> optional = Peer.state.chunkReplicationDegreeMap.get(identifier).stream().findFirst();
                if (optional.isPresent()) {
                    destinationAddress = optional.get();
                    System.out.println(destinationAddress);
                    Peer.executor.execute(this);
                    return;
                }
            }

            if (message instanceof FindSuccessorMessage) {
                // If a FindSuccessorMessage failed, attempt to contact the previous finger
                boolean resend = false;

                for (int i = 1; i < ChordNode.keyBits; ++i) {
                    if (chordNode.fingerTable.get(i).address.equals(destinationAddress)) {
                        resend = true;
                        destinationAddress = chordNode.fingerTable.get(i - 1).address;
                        break;
                    }
                }

                if (resend) {
                    Peer.executor.execute(this);
                }
            }
        }
    }

    private void sendMessage(SocketChannel channel, SSLEngine engine) throws IOException {
        byte[] messageBytes = message.build();

        ByteBuffer appData = ByteBuffer.wrap(messageBytes),
                netData = ByteBuffer.allocate(Math.max(appData.capacity(), engine.getSession().getPacketBufferSize()));

        while (appData.hasRemaining()) {
            netData.clear();

            // Attempt to encode message data
            SSLEngineResult result = engine.wrap(appData, netData);

            switch (result.getStatus()) {
                case OK:
                    netData.flip();
                    while (netData.hasRemaining()) {
                        channel.write(netData);
                    }
                    break;
                case BUFFER_OVERFLOW:
                    netData = increaseBufferCapacity(netData, engine.getSession().getPacketBufferSize());
                    break;
                case BUFFER_UNDERFLOW:
                    break;
                case CLOSED:
                    closeConnection(channel, engine);
                    //System.out.println("Closing connection...");
                    break;
            }
        }

        channel.shutdownOutput();
    }
}
