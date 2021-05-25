package chord;

import jsse.ClientThread;
import messages.GetPredecessorMessage;
import messages.NotifyMessage;
import protocol.Peer;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Thread that is executed periodically. A node asks its successor for its predecessor p and decides if
 * p should be the node's successor instead. This thread also notifies the node's successor of its existence.
 */
public class StabilizationThread extends Thread {
    @Override
    public void run() {
        ChordNode chordNode = Peer.state.chordNode;

        GetPredecessorMessage getPredecessorMessage = new GetPredecessorMessage(Peer.version, Peer.id, chordNode.selfInfo);
        NotifyMessage notifyMessage = new NotifyMessage(Peer.version, Peer.id, chordNode.selfInfo);

        try {
            ClientThread getPredecessorThread = new ClientThread(chordNode.getSuccessorInfo().address, getPredecessorMessage);
            Peer.executor.execute(getPredecessorThread);

            ClientThread notifyThread = new ClientThread(chordNode.getSuccessorInfo().address, notifyMessage);
            Peer.executor.execute(notifyThread);
        }
        catch (IOException | GeneralSecurityException ex) {
            System.out.println("Exception when sending NOTIFY message: " + ex.getMessage());
        }
    }
}
