package jsse;

import messages.Message;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

public class ClientThread extends SSLThread {
    private final InetSocketAddress destinationAddress;
    private final Message message;

    public ClientThread(InetSocketAddress destinationAddress, Message message) throws GeneralSecurityException, IOException {
        super("SSL", "client.keys", "truststore", "123456");

        this.destinationAddress = destinationAddress;
        this.message = message;
    }

    @Override
    public void run() {
        SSLEngine engine = context.createSSLEngine(destinationAddress.getHostName(), destinationAddress.getPort());
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
            ex.printStackTrace();
        }
    }

    private void sendMessage(SocketChannel channel, SSLEngine engine) throws IOException {
        byte[] messageBytes = message.build();

        ByteBuffer appData = ByteBuffer.wrap(messageBytes),
                netData = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());

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
                case CLOSED:
                case BUFFER_OVERFLOW:
                case BUFFER_UNDERFLOW:
                    break;
            }
        }
    }
}
