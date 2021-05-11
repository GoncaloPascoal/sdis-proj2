package chord;

import java.net.InetAddress;

public class ChordNodeInfo {
    public final int id;
    public final InetAddress address;
    public final int port;

    public ChordNodeInfo(int id, InetAddress address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }
}
