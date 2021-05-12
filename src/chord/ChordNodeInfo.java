package chord;

import java.net.InetSocketAddress;

public class ChordNodeInfo {
    public final int id;
    public final InetSocketAddress address;

    public ChordNodeInfo(int id, InetSocketAddress address) {
        this.id = id;
        this.address = address;
    }

    @Override
    public String toString() {
        return "#" + id + " (" + address.getHostName() + ":" + address.getPort() + ")";
    }
}
