package jsse;

import javax.net.ssl.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SSLThread extends Thread {
    protected final SSLContext context;

    public SSLThread(String protocol, String keyStorePath, String trustStorePath, String password) throws GeneralSecurityException, IOException {
        context = SSLContext.getInstance(protocol);
        context.init(getKeyManagers(keyStorePath, password), getTrustManagers(trustStorePath, password), new SecureRandom());
    }

    // Adapted from https://docs.oracle.com/en/java/javase/11/security/java-secure-socket-extension-jsse-reference-guide.html
    public void doHandshake(SocketChannel channel, SSLEngine engine) throws IOException {
        int bufferSize = engine.getSession().getApplicationBufferSize();

        ByteBuffer netData = ByteBuffer.allocate(bufferSize),
                peerNetData = ByteBuffer.allocate(bufferSize),
                appData = ByteBuffer.allocate(bufferSize),
                peerAppData = ByteBuffer.allocate(bufferSize);

        engine.beginHandshake();
        SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
        SSLEngineResult result;

        //System.out.println("Started handshake, status: " + status);

        while (status != SSLEngineResult.HandshakeStatus.FINISHED &&
            status != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {

            switch (status) {
                case NEED_UNWRAP:
                    // Data from peer needs to be received before the handshaking process can continue
                    if (channel.read(peerNetData) < 0) {
                        // Channel has reached end-of-stream
                        if (engine.isInboundDone() && engine.isOutboundDone()) {
                            System.out.println("Handshake failed");
                            return;
                        }

                        try {
                            engine.closeInbound();
                        }
                        catch (SSLException ex) {
                            System.out.println("Couldn't close inbound: " + ex.getMessage());
                        }

                        engine.closeOutbound();
                        status = engine.getHandshakeStatus();
                        break;
                    }

                    peerNetData.flip();
                    result = engine.unwrap(peerNetData, peerAppData);
                    peerNetData.compact();

                    status = result.getHandshakeStatus();

                    //System.out.println("Unwrap, result: " + result.getStatus() + "\n - Bytes consumed: " + result.bytesConsumed());

                    switch (result.getStatus()) {
                        case OK:
                            break;
                        case CLOSED:
                            if (engine.isOutboundDone()) {
                                return;
                            }
                            else {
                                engine.closeOutbound();
                                status = engine.getHandshakeStatus();
                            }
                            break;
                        case BUFFER_OVERFLOW:
                            peerAppData = increaseBufferCapacity(peerAppData, engine.getSession().getApplicationBufferSize());
                            break;
                        case BUFFER_UNDERFLOW: {
                            // Not enough data to process
                            int newSize = engine.getSession().getPacketBufferSize();

                            if (newSize > peerNetData.capacity()) {
                                peerNetData = ByteBuffer.allocate(newSize);
                            }
                            break;
                        }
                    }

                    break;
                case NEED_WRAP:
                    // Data needs to be sent to peer before the handshaking process can continue
                    netData.clear();

                    result = engine.wrap(appData, netData);
                    status = result.getHandshakeStatus();

                    //System.out.println("Wrap, result: " + result.getStatus() + "\n - Bytes produced: " + result.bytesProduced());

                    switch (result.getStatus()) {
                        case OK:
                            netData.flip();

                            while (netData.hasRemaining()) {
                                channel.write(netData);
                            }
                            break;
                        case CLOSED:
                            netData.flip();
                            while (netData.hasRemaining()) {
                                channel.write(netData);
                            }
                            peerNetData.clear();
                            break;
                        case BUFFER_OVERFLOW:
                            // Buffer is not large enough, enlarge the network buffer
                            netData = increaseBufferCapacity(netData, engine.getSession().getApplicationBufferSize());
                            break;
                        case BUFFER_UNDERFLOW:
                            break;
                    }
                    break;
                case NEED_TASK:
                    // The results of one or more tasks are needed before the handshaking process can continue
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        task.run();
                    }

                    status = engine.getHandshakeStatus();
                    break;
                default:
                    break;
            }
        }

        System.out.println("Handshake complete");
    }

    protected void closeConnection(SocketChannel channel, SSLEngine engine) throws IOException {
        engine.closeOutbound();
        System.out.println("Closing connection...");

        int bufferSize = engine.getSession().getPacketBufferSize();

        ByteBuffer empty = ByteBuffer.allocate(bufferSize),
                netData = ByteBuffer.allocate(bufferSize);

        while (!engine.isOutboundDone()) {
            SSLEngineResult result = engine.wrap(empty, netData);

            switch (result.getStatus()) {
                case OK:
                case BUFFER_UNDERFLOW: // Should never happen since we are calling wrap
                    break;
                case BUFFER_OVERFLOW:
                    if (engine.getSession().getPacketBufferSize() > netData.capacity()) {
                        netData = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
                    }
                    else {
                        netData.compact();
                    }
                    break;
                case CLOSED:
                    netData.flip();
                    while (netData.hasRemaining()) {
                        channel.write(netData);
                    }
                    break;
            }

        }

        channel.close();
    }

    protected ByteBuffer increaseBufferCapacity(ByteBuffer buffer, int newSize) {
        if (newSize > buffer.capacity()) {
            buffer = ByteBuffer.allocate(newSize);
        }
        else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }

        return buffer;
    }

    private KeyManager[] getKeyManagers(String keyStorePath, String password) throws GeneralSecurityException, IOException {
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        File clientKeys = new File(keyStorePath);
        KeyStore keyStore = KeyStore.getInstance(clientKeys, password.toCharArray());

        factory.init(keyStore, password.toCharArray());
        return factory.getKeyManagers();
    }

    private TrustManager[] getTrustManagers(String trustStorePath, String password) throws GeneralSecurityException, IOException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        File trustStoreFile = new File(trustStorePath);
        KeyStore trustStore = KeyStore.getInstance(trustStoreFile, password.toCharArray());

        factory.init(trustStore);
        return factory.getTrustManagers();
    }
}
