package chord;

import java.net.InetSocketAddress;

public class ChordNodeInfo {
    public final long id;
    public final InetSocketAddress address;

    public ChordNodeInfo(long id, InetSocketAddress address) {
        this.id = id;
        this.address = address;
    }

    @Override
    public String toString() {
        return "#" + id + " (" + address.getAddress().getHostAddress() + ":" + address.getPort() + ")";
    }
}
