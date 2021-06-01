package chord;

import jsse.ClientThread;
import messages.GetSuccessorMessage;
import protocol.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;

public class FindSuccessorsThread extends Thread {
    public static boolean procedureFinished = true;

    @Override
    public void run() {
        if (!procedureFinished) {
            // If the last procedure of this type hasn't finished yet, abort
            return;
        }

        ChordNode chordNode = Peer.state.chordNode;

        if (chordNode.successorDeque.size() < ChordNode.numSuccessors) {
            procedureFinished = false;

            // Find out the successor of the last node in the successor deque
            GetSuccessorMessage message = new GetSuccessorMessage(Peer.version, Peer.id, Peer.address);
            InetSocketAddress address = chordNode.getSuccessorInfo().address;
            try {
                if (!chordNode.successorDeque.isEmpty()) address = chordNode.successorDeque.getLast().address;
                ClientThread thread = new ClientThread(address, message);
                Peer.executor.execute(thread);
            }
            catch (IOException | GeneralSecurityException ex) {
                System.err.println("Error when sending GET_SUCCESSOR message: " + ex.getMessage());
            }
        }
    }
}
