package jsse;

import messages.Message;
import protocol.Peer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

public class ClientThread extends SSLThread {
    private final InetSocketAddress destinationAddress;
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
            //closeConnection(socketChannel, engine);
        }
        catch (IOException ex) {
            System.out.println("Exception in JSSE Client: " + ex.getMessage());
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
                    System.out.println("Message sent");
                    break;
                case BUFFER_OVERFLOW:
                    netData = increaseBufferCapacity(netData, engine.getSession().getPacketBufferSize());
                    break;
                case BUFFER_UNDERFLOW:
                    break;
                case CLOSED:
                    closeConnection(channel, engine);
                    System.out.println("Closing connection...");
                    break;
            }
        }
    }
}
