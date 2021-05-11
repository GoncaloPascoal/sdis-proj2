package protocol;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import chord.ChordNode;
import client.ClientInterface;

public class Peer extends ChordNode implements ClientInterface {
    public static String version;
    public static int id;

    public void backup(String filePath, int replicationDegree) throws RemoteException {

    }

    public void restore(String filePath) throws RemoteException {

    }

    public void delete(String filePath) throws RemoteException {

    }

    public void reclaim(long diskSpace) throws RemoteException {

    }

    public static void printUsage() {
        System.out.println("Usage: protocol.Peer <protocol_version> <peer_id> <service_ap> <port> [chord_addr chord_port].");
        System.out.println("When run without the last two arguments, a new Chord network is created.");
        System.out.println("Otherwise, the protocol.Peer will join the network specified by chord_addr:chord_port.");
    }

    public static void main(String[] args) {
        if (args.length != 4 && args.length != 6) {
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
        int port = Integer.parseInt(args[3]);
        InetSocketAddress address = new InetSocketAddress("localhost", port), chordAddress;

        if (args.length == 6) {
            // Joining an existing Chord network
            int chordPort = Integer.parseInt(args[5]);
            chordAddress = new InetSocketAddress(args[4], chordPort);

            if (chordAddress.isUnresolved()) {
                System.err.println("Invalid hostname: '" + args[4] + "'");
                return;
            }
        }
        else {
            // Creating a new Chord network
        }

        System.out.println("Successfully joined the network.");
    }
}
