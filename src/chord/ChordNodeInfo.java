package chord;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Objects;

public class ChordNodeInfo implements Serializable {
    private static final long serialVersionUID = 1L;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChordNodeInfo other = (ChordNodeInfo) o;
        return id == other.id && Objects.equals(address, other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, address);
    }
}
