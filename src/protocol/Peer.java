package protocol;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import chord.ChordNode;
import client.ClientInterface;
import jsse.ServerThread;
import utils.Utils;

public class Peer implements ClientInterface {
    public static final int CHUNK_MAX_SIZE = 64000;
    public static final long FILE_MAX_SIZE = (long) CHUNK_MAX_SIZE * 1000000;

    public static String version;
    public static int id;
    public static InetSocketAddress address;

    public static String keyStorePath, trustStorePath, password;

    public static final int MAX_THREADS = 50;
    public static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(MAX_THREADS);

    public static PeerState state = new PeerState();

    @Override
    public void backup(String filePath, int replicationDegree) throws RemoteException {
        File file = new File(filePath);

        if (!file.exists()) {
            System.err.println("Error: specified file does not exist!");
            return;
        }

        if (file.length() > FILE_MAX_SIZE) {
            System.err.println("Error when backing up file: file size is greater than 64 GB");
            return;
        }

        if (replicationDegree < 1 || replicationDegree > 9) {
            System.err.println("Error: replication degree must be a digit between 1 and 9");
            return;
        }

        try {
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(filePath), StandardOpenOption.READ);

            String fileId = Utils.calculateFileId(file);
            int numChunks = (int) (file.length() / CHUNK_MAX_SIZE + 1);

            // TODO
        }
        catch (IOException ex) {
            System.err.println("Error when reading from file: " + ex.getMessage());
        }
        catch (NoSuchAlgorithmException ex) {
            System.err.println("Error when calculating file ID: " + ex.getMessage());
        }
    }

    @Override
    public void restore(String filePath) throws RemoteException {

    }

    @Override
    public void delete(String filePath) throws RemoteException {

    }

    @Override
    public void reclaim(long diskSpace) throws RemoteException {

    }

    @Override
    public PeerState state() throws RemoteException {
        return state;
    }

    public static void printUsage() {
        System.out.println("Usage: Peer <protocol_version> <peer_id> <service_ap> <keystore_path> <truststore_path> <password> <addr> <port> [chord_addr chord_port].");
        System.out.println("When run without the last two arguments, a new Chord network is created.");
        System.out.println("Otherwise, the protocol.Peer will join the network specified by chord_addr:chord_port.");
    }

    public static void main(String[] args) {
        if (args.length != 8 && args.length != 10) {
            printUsage();
            return;
        }

        version = args[0];
        id = Integer.parseInt(args[1]);

        try {
            // RMI Setup
            Peer peer = new Peer();
            ClientInterface stub = (ClientInterface) UnicastRemoteObject.exportObject(peer, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(args[2], stub);
        }
        catch (RemoteException ex) {
            System.err.println("Exception occurred while setting up RMI: " + ex.getMessage());
        }

        // Keystore Setup
        keyStorePath = args[3];
        trustStorePath = args[4];
        password = args[5];

        // Chord Setup
        address = new InetSocketAddress(args[6], Integer.parseInt(args[7]));
        if (address.isUnresolved()) {
            System.err.println("Invalid hostname: '" + args[6] + "'");
            return;
        }

        // Start server thread that will listen to messages sent to the specified address and port
        try {
            ServerThread serverThread = new ServerThread();
            executor.execute(serverThread);
        }
        catch (GeneralSecurityException ex) {
            System.out.println("Security exception when creating server thread: " + ex.getMessage());
            return;
        }
        catch (IOException ex) {
            System.out.println("IO exception when creating server thread: " + ex.getMessage());
            return;
        }

        state.chordNode = new ChordNode(address);

        if (args.length == 10) {
            // Joining an existing Chord network
            InetSocketAddress contactAddress = new InetSocketAddress(args[8], Integer.parseInt(args[9]));

            if (contactAddress.isUnresolved()) {
                System.err.println("Invalid hostname: '" + args[8] + "'");
                return;
            }

            System.out.println("Joining existing network, will contact peer at: " + contactAddress.getAddress().getHostAddress() + ":" + contactAddress.getPort());
            state.chordNode.initializeFingerTable(contactAddress);
        }
        else {
            // Creating a new Chord network
            state.chordNode.initializeFingerTable();

            System.out.println("Successfully joined the network with id = " + state.chordNode.selfInfo.id + ".");
            System.out.println("Your successor is " + state.chordNode.getSuccessorInfo() + ".");
        }
    }
}
