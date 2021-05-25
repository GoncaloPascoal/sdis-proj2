package protocol;

import chord.ChordNode;
import chord.ChordNodeInfo;
import jsse.ClientThread;
import messages.FindSuccessorMessage;
import messages.SuccessorMessage;
import utils.Utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Queue;

public class HandleReceivedMessageThread extends Thread {
    private final byte[] messageBytes;

    public HandleReceivedMessageThread(byte[] messageBytes) {
        this.messageBytes = messageBytes;
    }

    @Override
    public void run() {
        List<byte[]> headerAndBody = Utils.splitMessage(messageBytes);

        if (headerAndBody.size() == 2) {
            String header = new String(headerAndBody.get(0));
            byte[] body = headerAndBody.get(1);

            String[] headerComponents = header.split(" ");
            if (headerComponents.length >= 2) {
                switch (headerComponents[1]) {
                    case "FIND_SUCCESSOR": {
                        System.out.println("Received FIND_SUCCESSOR message\n");
                        FindSuccessorMessage message = FindSuccessorMessage.parse(header, body);
                        if (message != null) handleFindSuccessorMessage(message);
                        break;
                    }
                    case "SUCCESSOR": {
                        System.out.println("Received SUCCESSOR message\n");
                        SuccessorMessage message = SuccessorMessage.parse(header, body);
                        if (message != null) handleSuccessorMessage(message);
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }

    private void handleFindSuccessorMessage(FindSuccessorMessage message) {
        ChordNode chordNode = Peer.state.chordNode;

        System.out.println(message.buildHeader());

        long start = chordNode.selfInfo.id;
        long end = chordNode.getSuccessorInfo().id;

        ChordNodeInfo closestPrecedingNode = chordNode.getClosestPrecedingNode(message.key);

        if ((message.key > start && message.key <= end)
                || (start > end && (message.key > start || message.key <= end))
                || closestPrecedingNode.equals(chordNode.selfInfo)) {
            try {
                ClientThread thread = new ClientThread(message.initiatorAddress,
                        new SuccessorMessage(Peer.version, Peer.id, message.key, chordNode.selfInfo));
                Peer.executor.execute(thread);
            }
            catch (IOException | GeneralSecurityException ex) {
                System.out.println("Exception occurred when handling FIND_SUCCESSOR message: " + ex.getMessage());
            }
            return;
        }

        message.senderId = Peer.id;

        try {
            ClientThread thread = new ClientThread(closestPrecedingNode.address, message);
            Peer.executor.execute(thread);
        }
        catch (IOException | GeneralSecurityException ex) {
            System.out.println("Exception occurred when handling FIND_SUCCESSOR message: " + ex.getMessage());
        }
    }

    public void handleSuccessorMessage(SuccessorMessage message) {
        ChordNode chordNode = Peer.state.chordNode;

        long keyDifference = message.key - chordNode.selfInfo.id;
        if (keyDifference > 0 && (keyDifference & -keyDifference) == keyDifference) {
            // The key difference is a power of two, update the finger table
            int index = (int) Math.round(Math.log(keyDifference) / Math.log(2));
            chordNode.fingerTable.set(index, message.nodeInfo);
        }

        if (chordNode.tasksMap.containsKey(message.key)) {
            Queue<Runnable> taskQueue = chordNode.tasksMap.get(message.key);

            while (!taskQueue.isEmpty()) {
                Runnable task = taskQueue.remove();
                Peer.executor.execute(task);
            }
        }
    }
}
