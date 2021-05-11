package chord;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ChordNode {
    // With an m-bit key, there can be 2^m nodes, and each has m entries in its finger table
    public static final int keyBits = 16;

    public int id;
    public InetSocketAddress address;
    // AtomicReferenceArray is used to ensure thread safety
    public AtomicReferenceArray<InetSocketAddress> fingerTable = new AtomicReferenceArray<>(keyBits);

    /**
     * Returns the address from the finger table that corresponds to the specified key.
     */
    public InetSocketAddress getFinger(int key) {
        int maxNodes = (int) Math.pow(2, keyBits);

        for (int i = 0; i < keyBits; ++i) {
            int start = (id + (int) Math.pow(2, i)) % maxNodes;
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
}
