package chord;

import jsse.ClientThread;
import messages.AliveMessage;
import messages.GetPredecessorMessage;
import messages.NotifyMessage;
import protocol.Peer;

/**
 * Thread that is executed periodically. A node asks its successor for its predecessor p and decides if
 * p should be the node's successor instead. This thread also notifies the node's successor of its existence.
 * Finally, it also verifies if the node's predecessor is still operational.
 */
public class StabilizationThread extends Thread {
    @Override
    public void run() {
        ChordNode chordNode = Peer.state.chordNode;

        GetPredecessorMessage getPredecessorMessage = new GetPredecessorMessage(Peer.version, Peer.id, chordNode.selfInfo);
        NotifyMessage notifyMessage = new NotifyMessage(Peer.version, Peer.id, chordNode.selfInfo);

        try {
            if (!chordNode.getSuccessorInfo().equals(chordNode.selfInfo)) {
                ClientThread getPredecessorThread = new ClientThread(chordNode.getSuccessorInfo().address, getPredecessorMessage);
                Peer.executor.execute(getPredecessorThread);

                ClientThread notifyThread = new ClientThread(chordNode.getSuccessorInfo().address, notifyMessage);
                Peer.executor.execute(notifyThread);

                AliveMessage aliveMessage = new AliveMessage(Peer.version, Peer.id);
                if (chordNode.predecessorInfo != null) {
                    ClientThread aliveThread = new ClientThread(chordNode.predecessorInfo.address, aliveMessage);
                    Peer.executor.execute(aliveThread);
                }
            }
            else {
                chordNode.stabilize(chordNode.predecessorInfo);
            }
        }
        catch (Exception ex) {
            System.err.println("Exception occurred when stabilizing: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
