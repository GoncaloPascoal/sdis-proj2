package chord;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class ChordNodeInfo implements Serializable {
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
