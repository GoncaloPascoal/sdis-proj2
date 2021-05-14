package jsse;

import protocol.Peer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

public class ServerThread extends SSLThread {
    private final ServerSocketChannel serverSocketChannel;

    public ServerThread() throws GeneralSecurityException, IOException {
        super("SSL", Peer.keyStorePath, Peer.trustStorePath, Peer.password);

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(Peer.address);
    }

    @Override
    public void run() {
        System.out.println("Starting server, will listen at: " + Peer.address.getAddress().getHostAddress() + ":" + Peer.address.getPort());
        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);

        while (true) {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                System.out.println("Accepted new connection.");

                doHandshake(socketChannel, engine);
                byte[] messageBytes = receiveMessage(socketChannel, engine);

                System.out.println(new String(messageBytes));
            }
            catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private byte[] receiveMessage(SocketChannel channel, SSLEngine engine) throws IOException {
        ByteBuffer peerAppData = ByteBuffer.allocate(65000),
                peerNetData = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        
        int bytesRead;
        if ((bytesRead = channel.read(peerNetData)) > 0) {
            peerNetData.flip();

            while (peerNetData.hasRemaining()) {
                // Attempts to decode message data
                SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
    
                switch (result.getStatus()) {
                    case OK:
                        break;
                    default:
                        break;
                }
            }
        }

        System.out.println(bytesRead);

        byte[] messageBytes = new byte[bytesRead];
        System.arraycopy(peerAppData.array(), 0, messageBytes, 0, bytesRead);

        return messageBytes;
    }
}
