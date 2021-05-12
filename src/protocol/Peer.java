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
import java.security.NoSuchAlgorithmException;

import chord.ChordNode;
import client.ClientInterface;
import utils.Utils;

public class Peer extends ChordNode implements ClientInterface {
    public static final int CHUNK_MAX_SIZE = 64000;
    public static final long FILE_MAX_SIZE = (long) CHUNK_MAX_SIZE * 1000000;

    public static String version;
    public static int id;
    public static InetSocketAddress address;

    public static PeerState state;

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
        System.out.println("Usage: Peer <protocol_version> <peer_id> <service_ap> <addr> <port> [chord_addr chord_port].");
        System.out.println("When run without the last two arguments, a new Chord network is created.");
        System.out.println("Otherwise, the protocol.Peer will join the network specified by chord_addr:chord_port.");
    }

    public static void main(String[] args) {
        if (args.length != 5 && args.length != 7) {
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

        // Chord Setup
        int port = Integer.parseInt(args[4]);
        InetSocketAddress address = new InetSocketAddress(args[4], port), chordAddress;

        if (args.length == 7) {
            // Joining an existing Chord network
            int chordPort = Integer.parseInt(args[6]);
            chordAddress = new InetSocketAddress(args[5], chordPort);

            if (chordAddress.isUnresolved()) {
                System.err.println("Invalid hostname: '" + args[5] + "'");
                return;
            }
        }
        else {
            // Creating a new Chord network
        }

        System.out.println("Successfully joined the network.");
    }
}
