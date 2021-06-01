package jsse;

import chord.ChordNode;
import chord.ChordNodeInfo;
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
