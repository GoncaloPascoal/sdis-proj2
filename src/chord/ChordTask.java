package chord;

import protocol.Peer;

/**
 * Task to be executed after information about a certain chord node is obtained (for example, after finding a key's
 * successor).
 */
public abstract class ChordTask {
    public void execute(ChordNodeInfo nodeInfo) {
        Peer.executor.execute(() -> performTask(nodeInfo));
    }

    public abstract void performTask(ChordNodeInfo nodeInfo);
}
