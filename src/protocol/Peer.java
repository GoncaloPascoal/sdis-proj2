package protocol;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

import chord.ChordNode;
import client.ClientInterface;
import jsse.ClientThread;
import jsse.ServerThread;
import messages.DeleteMessage;
import messages.GetChunkMessage;
import utils.Utils;
import workers.ReadChunkThread;
import workers.RemoveChunkThread;
import workers.StoreChunkThread;

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

    // List of chunks that have to be read from the file before the corresponding AsynchronousFileChannel is closed
    public static final ConcurrentHashMap<String, Set<Integer>> chunksToReadMap = new ConcurrentHashMap<>();

    // List of chunks that have to be written to the restored file before the corresponding AsynchronousFileChannel is closed
    public static final ConcurrentHashMap<String, Set<Integer>> chunksToRestoreMap = new ConcurrentHashMap<>();

    // Maps file IDs to their respective AsynchronousFileChannel, which is used by worker threads when restoring the file
    public static final ConcurrentHashMap<String, AsynchronousFileChannel> restoredFileChannelMap = new ConcurrentHashMap<>();

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

            chunksToReadMap.put(fileId, new HashSet<>());

            for (int chunkNumber = 0; chunkNumber < numChunks; ++chunkNumber) {
                ChunkIdentifier identifier = new ChunkIdentifier(fileId, chunkNumber);
                //state.backupChunks.add(identifier);
                chunksToReadMap.get(fileId).add(chunkNumber);
                state.chunkReplicationDegreeMap.put(identifier, Collections.synchronizedSet(new LinkedHashSet<>()));
            }

            for (int chunkNumber = 0; chunkNumber < numChunks; ++chunkNumber) {
                ReadChunkThread thread = new ReadChunkThread(channel, fileId, chunkNumber, replicationDegree);
                executor.execute(thread);
            }

            try {
                state.backupFilesMap.put(file.getCanonicalPath(), new FileInformation(fileId, replicationDegree, numChunks));
            }
            catch (IOException ex) {
                System.err.println("Error when converting to canonical file path: " + ex.getMessage());
            }
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
        File file = new File(filePath);

        FileInformation information;
        try {
            information = state.backupFilesMap.get(file.getCanonicalPath());
        }
        catch (IOException ex) {
            System.err.println("Error when converting to canonical file path: " + ex.getMessage());
            return;
        }

        if (information == null) {
            System.err.println("Error: the specified file wasn't backed up by this peer");
            return;
        }

        Path path = Paths.get("peer" + id + File.separator + "restored" + File.separator + information.fileId + File.separator + file.getName());
        path.toFile().getParentFile().mkdirs();

        chunksToRestoreMap.put(information.fileId, ConcurrentHashMap.newKeySet());

        try {
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            restoredFileChannelMap.put(information.fileId, channel);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        for (int chunkNumber = 0; chunkNumber < information.numChunks; ++chunkNumber) {
            GetChunkMessage message = new GetChunkMessage(Peer.version, Peer.id, information.fileId, chunkNumber, Peer.address);
            ChunkIdentifier identifier = new ChunkIdentifier(information.fileId, chunkNumber);
            InetSocketAddress firstPeerAddress = state.chunkReplicationDegreeMap.get(identifier).iterator().next();

            if (firstPeerAddress == null) {
                System.err.println("Error: no peer has backed up chunk " + chunkNumber + " of file with id " + information.fileId);
                return;
            }

            try {
                ClientThread thread = new ClientThread(firstPeerAddress, message);
                executor.execute(thread);
            }
            catch (IOException | GeneralSecurityException ex) {
                System.err.println("Error when sending GET_CHUNK message: " + ex.getMessage());
            }
        }
    }

    @Override
    public void delete(String filePath) throws RemoteException {
        File file = new File(filePath);

        FileInformation information;
        try {
            String canonicalPath = file.getCanonicalPath();
            information = state.backupFilesMap.get(canonicalPath);
            state.backupFilesMap.remove(canonicalPath);
        }
        catch (IOException ex) {
            System.out.println("Error when converting to canonical file path: " + ex.getMessage());
            return;
        }

        if (information == null) {
            System.err.println("Error: the specified file wasn't backed up by this peer");
            return;
        }

        String fileId = information.fileId;
        DeleteMessage message = new DeleteMessage(Peer.version, Peer.id, fileId);

        Set<InetSocketAddress> peers = new HashSet<>();

        for (int chunkNumber = 0; chunkNumber < information.numChunks; ++chunkNumber) {
            ChunkIdentifier identifier = new ChunkIdentifier(fileId, chunkNumber);
            peers.addAll(state.chunkReplicationDegreeMap.get(identifier));
            state.chunkReplicationDegreeMap.remove(identifier);
        }

        for (InetSocketAddress address : peers) {
            try {
                ClientThread thread = new ClientThread(address, message);
                Peer.executor.execute(thread);
            }
            catch (IOException | GeneralSecurityException ex) {
                System.err.println("Error when sending DELETE message: " + ex.getMessage());
            }
        }
    }

    @Override
    public void reclaim(long diskSpace) throws RemoteException {
        if (diskSpace < 0) return;

        // TODO: try to move as much code out of the synchronized block as possible to improve concurrency
        synchronized (StoreChunkThread.lock) {
            state.maxDiskSpace = diskSpace * 1000; // diskSpace is specified in KBytes
            long spaceOccupied = state.getSpaceOccupied();

            if (spaceOccupied > state.maxDiskSpace) {
                long spaceToFree = spaceOccupied - state.maxDiskSpace, spaceFreed = 0;

                Set<Map.Entry<ChunkIdentifier, ChunkInformation>> entrySet = state.storedChunksMap.entrySet();
                List<Map.Entry<ChunkIdentifier, ChunkInformation>> sortedEntries = entrySet.stream()
                        .sorted(Comparator.comparingInt(e -> e.getValue().size))
                        .collect(Collectors.toList());

                Collections.reverse(sortedEntries); // Reverse sorting so that the largest chunks are first

                for (int i = sortedEntries.size() - 1; i >= 0; --i) {
                    Map.Entry<ChunkIdentifier, ChunkInformation> entry = sortedEntries.get(i);
                    ChunkIdentifier identifier = entry.getKey();
                    InetSocketAddress initiatorAddress = entry.getValue().initiatorAddress;

                    System.out.println("Deleting chunk " + identifier.fileId + " | " + identifier.chunkNumber);

                    spaceFreed += entry.getValue().size;
                    state.storedChunksMap.remove(identifier);

                    // Create thread to free disk space and send the REMOVED message
                    RemoveChunkThread thread = new RemoveChunkThread(identifier, initiatorAddress);
                    executor.execute(thread);

                    if (spaceFreed >= spaceToFree) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public PeerState state() throws RemoteException {
        return state;
    }

    public static void printUsage() {
        System.out.println("Usage: Peer <protocol_version> <peer_id> <service_ap> <client_keys_path> <server_keys_path> <truststore_path> <password> <addr> <port> [chord_addr chord_port].");
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
            return;
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
            state.chordNode.joinNetwork(contactAddress);
        }
        else {
            // Creating a new Chord network
            state.chordNode.joinNetwork();

            System.out.println("Successfully joined the network with id = " + state.chordNode.selfInfo.id + ".");
            System.out.println("Your successor is " + state.chordNode.getSuccessorInfo() + ".");
        }
    }
}
