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

    public ServerThread(String keyStorePath, String trustStorePath, String password) throws GeneralSecurityException, IOException {
        super("SSL", keyStorePath, trustStorePath, password);

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(Peer.address);
    }

    @Override
    public void run() {
        System.out.println("Starting server, will listen at: " + Peer.address.getHostName() + ":" + Peer.address.getPort());

        while (true) {
            SSLEngine engine = context.createSSLEngine();
            engine.setUseClientMode(false);

            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                doHandshake(socketChannel, engine);
                byte[] messageBytes = receiveMessage(socketChannel, engine);

                System.out.println(new String(messageBytes));
            }
            catch (IOException ex) {
                ex.printStackTrace();
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
