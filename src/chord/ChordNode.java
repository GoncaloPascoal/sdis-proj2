package chord;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ChordNode {
    // With an m-bit key, there can be 2^m nodes, and each has m entries in its finger table
    public static final int keyBits = 16;

    public ChordNodeInfo selfInfo, predecessorInfo;
    // AtomicReferenceArray is used to ensure thread safety
    public AtomicReferenceArray<ChordNodeInfo> fingerTable = new AtomicReferenceArray<>(keyBits);

    public ChordNode(InetSocketAddress address) {
        try {
            int maxNodes = (int) Math.pow(2, keyBits);

            String input = address.getHostName() + ":" + address.getPort();
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            String hashed = new String(digest.digest(input.getBytes()));

            selfInfo = new ChordNodeInfo(Math.abs(hashed.hashCode()) % maxNodes, address);
        }
        catch (NoSuchAlgorithmException ex) {
            System.out.println("Algorithm does not exist: " + ex.getMessage());
        }
    }

    /**
     * Returns information from the node in the finger table that corresponds to the specified key.
     */
    public ChordNodeInfo getFinger(int key) {
        int maxNodes = (int) Math.pow(2, keyBits);

        for (int i = 0; i < keyBits; ++i) {
            int start = (selfInfo.id + (int) Math.pow(2, i)) % maxNodes;
            int end = (start + (int) Math.pow(2, i) - 1) % maxNodes;

            if (start <= end) {
                if (key >= start && key <= end) {
                    return fingerTable.get(i);
                }
            }
            else {
                if (key >= start || key <= end) {
                    return fingerTable.get(i);
                }
            }
        }

        return null;
    }

    public ChordNodeInfo getSuccessorInfo() {
        return fingerTable.get(0);
    }

    public void setSuccessorInfo(ChordNodeInfo info) {
        fingerTable.set(0, info);
    }
}
