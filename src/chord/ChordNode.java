package chord;

import jsse.ClientThread;
import messages.FindSuccessorMessage;
import protocol.CheckReplicationDegreeThread;
import protocol.Peer;
import protocol.VerifyChunksThread;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ChordNode implements Serializable {
    private static final long serialVersionUID = 1L;

    // With an m-bit key, there can be 2^m nodes, and each has m entries in its finger table
    public static final int keyBits = 10;
    public static final long maxNodes = (long) Math.pow(2, keyBits);

    // Fault tolerance: in addition to its successor, the peer keeps the addresses of n successors, so that it can
    // continue operating if its successor fails
    public static final int numSuccessors = 2;
    public final ConcurrentLinkedDeque<ChordNodeInfo> successorDeque = new ConcurrentLinkedDeque<>();

    public ChordNodeInfo selfInfo, predecessorInfo = null;
    // AtomicReferenceArray is used to ensure thread safety
    public AtomicReferenceArray<ChordNodeInfo> fingerTable = new AtomicReferenceArray<>(keyBits);

    public final ConcurrentHashMap<Long, Queue<ChordTask>> tasksMap = new ConcurrentHashMap<>();

    public ChordNode(InetSocketAddress address) {
        try {
            String input = address.getHostName() + ":" + address.getPort();
            long key = generateKey(input.getBytes());

            selfInfo = new ChordNodeInfo(key, address);
            System.out.println("Joining the network with id = " + selfInfo.id + ".");
        }
        catch (NoSuchAlgorithmException ex) {
            System.out.println("Algorithm does not exist: " + ex.getMessage());
        }
    }


    /**
     * Generates an m-bit key to be used for the Chord protocol from a sequence of bytes. The key is
     * generated using a consistent hashing algorithm, in this case SHA-1. The 160-bit SHA-1 has is truncated
     * to a length of m bits, using the least significant bits.
     */
    public static long generateKey(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] sha1Bytes = digest.digest(input);

        long key = 0;
        // The length of the key in bytes, rounded up (for example, a 20-bit key would need 3 bytes)
        int keyNumBytes = (int) Math.ceil((double) keyBits / 8);

        // Obtain the m least significant bits from the 160-bit SHA-1 hash
        // This is equivalent to obtaining the 160-bit hash modulo 2^m
        for (int i = 0; i < keyNumBytes; ++i) {
            byte b = sha1Bytes[sha1Bytes.length - 1 - i];
            key |= Byte.toUnsignedInt(b) << (8 * i);
        }

        // This bitmask is needed whenever m is not a multiple of 8,
        // since we need to guarantee that the generated keys are always smaller than 2^m
        long mask = (long) Math.pow(2, keyBits) - 1;
        key &= mask;

        return key;
    }

    public static boolean isKeyBetween(long key, long start, long end, boolean inclusiveStart, boolean inclusiveEnd) {
        if (start < end) {
            return (key > start && key < end) || (inclusiveStart && key == start) || (inclusiveEnd && key == end);
        }
        else if (start > end) {
            return (key > start || key < end) || (inclusiveStart && key == start) || (inclusiveEnd && key == end);
        }
        else { // start == end
            return key != start || inclusiveStart || inclusiveEnd;
        }
    }

    public static boolean isKeyBetween(long key, long start, long end) {
        return isKeyBetween(key, start, end, false, false);
    }


    public ChordNodeInfo getSuccessorInfo() {
        return fingerTable.get(0);
    }

    public void setSuccessorInfo(ChordNodeInfo info) {
        fingerTable.set(0, info);
    }

    private void startPeriodicTasks() {
        // Schedule FixFingersThread to execute periodically
        FixFingersThread fixFingersThread = new FixFingersThread();
        Peer.executor.scheduleWithFixedDelay(fixFingersThread, 0, 300, TimeUnit.MILLISECONDS);

        // Schedule StabilizationThread to execute periodically
        StabilizationThread stabilizationThread = new StabilizationThread();
        Peer.executor.scheduleWithFixedDelay(stabilizationThread, 0, 2, TimeUnit.SECONDS);

        // Schedule FindSuccessorsThread to execute periodically
        FindSuccessorsThread findSuccessorsThread = new FindSuccessorsThread();
        Peer.executor.scheduleWithFixedDelay(findSuccessorsThread, 0, 10, TimeUnit.SECONDS);

        // Tasks related to the backup service
        VerifyChunksThread verifyChunksThread = new VerifyChunksThread();
        Peer.executor.scheduleWithFixedDelay(verifyChunksThread, 0, 6, TimeUnit.SECONDS);

        // Tasks related to the backup service
        CheckReplicationDegreeThread checkReplicationDegreeThread = new CheckReplicationDegreeThread();
        Peer.executor.scheduleWithFixedDelay(checkReplicationDegreeThread, 0, 6, TimeUnit.SECONDS);
    }

    public void initializeFingerTable() {
        // Set all fingers to point to this node. Usually these will be overridden, but it ensures there will
        // be no null fingers in the finger table.
        for (int i = 0; i < fingerTable.length(); ++i) {
            fingerTable.set(i, selfInfo);
        }
    }

    public void joinNetwork() {
        // Called when the node is creating a new Chord network
        initializeFingerTable();
        startPeriodicTasks();
    }

    public void joinNetwork(InetSocketAddress contact) {
        // Called when the node is joining a new Chord network
        initializeFingerTable();

        long successorStart = getStartKey(0);

        tasksMap.putIfAbsent(successorStart, new ConcurrentLinkedQueue<>());
        tasksMap.get(successorStart).add(new ChordTask() {
            @Override
            public void performTask(ChordNodeInfo nodeInfo) {
                startPeriodicTasks();

                // Update the new node's finger table
                try {
                    for (int i = 1; i < keyBits; ++i) {
                        long fingerStart = selfInfo.id + (long) Math.pow(2, i);
                        FindSuccessorMessage message = new FindSuccessorMessage(Peer.version, Peer.id, fingerStart, Peer.address);
                        ClientThread thread = new ClientThread(contact, message);
                        Peer.executor.execute(thread);
                    }
                }
                catch (IOException | GeneralSecurityException ex) {
                    System.err.println("Error when updating new node's finger table: " + ex.getMessage());
                }
            }
        });

        FindSuccessorMessage message = new FindSuccessorMessage(Peer.version, Peer.id, successorStart, Peer.address);

        try {
            ClientThread thread = new ClientThread(contact, message);
            Peer.executor.execute(thread);
        }
        catch (IOException | GeneralSecurityException ex) {
            System.err.println("Error when sending FIND_SUCCESSOR message: " + ex.getMessage());
        }
    }

    /**
     * Returns the start key for the i-th finger (considering that indexing starts at 0).
     */
    public long getStartKey(int i) {
        return (selfInfo.id + (long) Math.pow(2, i)) % maxNodes;
    }

    /**
     * Returns information from the node in the finger table that most closely precedes the specified key.
     */
    public ChordNodeInfo getClosestPrecedingNode(long key) {
        for (int i = keyBits - 1; i >= 0; --i) {
            ChordNodeInfo finger = fingerTable.get(i);
            long fingerId = finger.id;

            if (fingerId > selfInfo.id && fingerId < key
                    || (selfInfo.id > key && (fingerId > selfInfo.id || fingerId < key))) {
                return finger;
            }
        }

        return selfInfo;
    }

    public void stabilize(ChordNodeInfo predecessorInfo) {
        if (predecessorInfo != null && ChordNode.isKeyBetween(predecessorInfo.id, selfInfo.id, getSuccessorInfo().id)) {
            setSuccessorInfo(predecessorInfo);
            System.out.println("Your successor is: " + getSuccessorInfo());
        }
    }
}
