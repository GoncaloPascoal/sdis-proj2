package jsse;

import messages.Message;
import protocol.HandleReceivedMessageThread;
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
        super("TLS", Peer.serverKeysPath, Peer.trustStorePath, Peer.password);

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(Peer.address);
    }

    @Override
    public void run() {
        System.out.println("Starting server, will listen at: " + Peer.address.getAddress().getHostAddress() + ":" + Peer.address.getPort());

        while (true) {
            try {
                SSLEngine engine = context.createSSLEngine();
                engine.setUseClientMode(false);

                SocketChannel socketChannel = serverSocketChannel.accept();
                //System.out.println("Accepted new connection.");

                doHandshake(socketChannel, engine);
                byte[] messageBytes = receiveMessage(socketChannel, engine);

                //System.out.println("Received message with length " + messageBytes.length + " bytes.");
                closeConnection(socketChannel, engine);

                HandleReceivedMessageThread thread = new HandleReceivedMessageThread(messageBytes);
                Peer.executor.execute(thread);
            }
            catch (IOException ex) {
                System.out.println("Exception in JSSE Server: " + ex.getMessage());
            }
        }
    }

    private byte[] receiveMessage(SocketChannel channel, SSLEngine engine) throws IOException {
        ByteBuffer peerAppData = ByteBuffer.allocate(Message.MAX_SIZE),
                peerNetData = ByteBuffer.allocate(Message.MAX_SIZE);
        
        int bytesRead;
        if ((bytesRead = channel.read(peerNetData)) > 0) {
            peerNetData.flip();

            while (peerNetData.hasRemaining()) {
                // Attempts to decode message data
                SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
    
                switch (result.getStatus()) {
                    case OK:
                        peerAppData.flip();
                        break;
                    case BUFFER_OVERFLOW:
                        peerNetData = increaseBufferCapacity(peerNetData, engine.getSession().getPacketBufferSize());
                        break;
                    case BUFFER_UNDERFLOW:
                        break;
                    case CLOSED:
                        //System.out.println("Closing connection...");
                        closeConnection(channel, engine);
                        break;
                }
            }
        }

        byte[] messageBytes = new byte[bytesRead];
        System.arraycopy(peerAppData.array(), 0, messageBytes, 0, bytesRead);

        return messageBytes;
    }
}
