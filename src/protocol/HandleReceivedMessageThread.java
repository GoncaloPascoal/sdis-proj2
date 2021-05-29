package protocol;

import chord.ChordNode;
import chord.ChordNodeInfo;
import chord.ChordTask;
import jsse.ClientThread;
import messages.*;
import utils.Utils;
import workers.StoreChunkThread;

import java.io.File;
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
                //System.out.println("Received " + headerComponents[1] + " message");
                switch (headerComponents[1]) {
                    case "FIND_SUCCESSOR": {
                        FindSuccessorMessage message = FindSuccessorMessage.parse(header, body);
                        if (message != null) handleFindSuccessorMessage(message);
                        break;
                    }
                    case "SUCCESSOR": {
                        SuccessorMessage message = SuccessorMessage.parse(header, body);
                        if (message != null) handleSuccessorMessage(message);
                        break;
                    }
                    case "GET_PREDECESSOR": {
                        GetPredecessorMessage message = GetPredecessorMessage.parse(header, body);
                        if (message != null) handleGetPredecessorMessage(message);
                        break;
                    }
                    case "PREDECESSOR": {
                        PredecessorMessage message = PredecessorMessage.parse(header, body);
                        if (message != null) handlePredecessorMessage(message);
                        break;
                    }
                    case "NOTIFY": {
                        NotifyMessage message = NotifyMessage.parse(header, body);
                        if (message != null) handleNotifyMessage(message);
                        break;
                    }
                    case "PUT_CHUNK": {
                        PutChunkMessage message = PutChunkMessage.parse(header, body);
                        if (message != null) handlePutChunkMessage(message);
                        break;
                    }
                    case "STORED": {
                        StoredMessage message = StoredMessage.parse(header);
                        if (message != null) handleStoredMessage(message);
                        break;
                    }
                    case "DELETE": {
                        DeleteMessage message = DeleteMessage.parse(header);
                        if (message != null) handleDeleteMessage(message);
                        break;
                    }
                    case "GET_CHUNK": {
                        GetChunkMessage message = GetChunkMessage.parse(header);
                        if (message != null) handleGetChunkMessage(message);
                        break;
                    }
                    case "CHUNK": {
                        ChunkMessage message = ChunkMessage.parse(header, body);
                        if (message != null) handleChunkMessage(message);
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

        long start = chordNode.selfInfo.id;
        long end = chordNode.getSuccessorInfo().id;

        if (ChordNode.isKeyBetween(message.key, start, end, false, true)) {
            try {
                ClientThread thread = new ClientThread(message.initiatorAddress,
                        new SuccessorMessage(Peer.version, Peer.id, message.key, chordNode.getSuccessorInfo()));
                Peer.executor.execute(thread);
            }
            catch (IOException | GeneralSecurityException ex) {
                System.err.println("Exception occurred when handling FIND_SUCCESSOR message: " + ex.getMessage());
            }
            return;
        }

        ChordNodeInfo closestPrecedingNode = chordNode.getClosestPrecedingNode(message.key);
        message.senderId = Peer.id;

        try {
            ClientThread thread = new ClientThread(closestPrecedingNode.address, message);
            Peer.executor.execute(thread);
        }
        catch (IOException | GeneralSecurityException ex) {
            System.err.println("Exception occurred when handling FIND_SUCCESSOR message: " + ex.getMessage());
        }
    }

    public void handleSuccessorMessage(SuccessorMessage message) {
        ChordNode chordNode = Peer.state.chordNode;

        // Since we are working with modular arithmetic, we need to take precautions when calculating keyDifference
        long keyDifference = message.key - chordNode.selfInfo.id;
        if (keyDifference < 0) keyDifference += ChordNode.maxNodes;

        if ((keyDifference & -keyDifference) == keyDifference) {
            // The key difference is a power of two, update the finger table
            int index = (int) Math.round(Math.log(keyDifference) / Math.log(2));
            chordNode.fingerTable.set(index, message.nodeInfo);
        }

        if (chordNode.tasksMap.containsKey(message.key)) {
            Queue<ChordTask> taskQueue = chordNode.tasksMap.get(message.key);

            while (!taskQueue.isEmpty()) {
                taskQueue.remove().execute(message.nodeInfo);
            }
        }
    }

    private void handleGetPredecessorMessage(GetPredecessorMessage message) {
        ChordNode chordNode = Peer.state.chordNode;

        PredecessorMessage predecessorMessage = new PredecessorMessage(Peer.version, Peer.id, chordNode.predecessorInfo);

        try {
            ClientThread thread = new ClientThread(message.nodeInfo.address, predecessorMessage);
            Peer.executor.execute(thread);
        }
        catch (IOException | GeneralSecurityException ex) {
            System.err.println("Exception occurred when handling GET_PREDECESSOR message: " + ex.getMessage());
        }
    }

    private void handlePredecessorMessage(PredecessorMessage message) {
        Peer.state.chordNode.stabilize(message.predecessorInfo);
    }

    private void handleNotifyMessage(NotifyMessage message) {
        ChordNode chordNode = Peer.state.chordNode;

        if (chordNode.predecessorInfo == null
                || ChordNode.isKeyBetween(message.nodeInfo.id, chordNode.predecessorInfo.id, chordNode.selfInfo.id)) {
            chordNode.predecessorInfo = message.nodeInfo;
            System.out.println("Your predecessor is " + chordNode.predecessorInfo);
        }
    }

    private void handlePutChunkMessage(PutChunkMessage message) {
        // The chunk cannot be stored by the initiator peer
        if (!message.initiatorAddress.equals(Peer.address)) {
            // Store the chunk and send a STORED message to initiator peer
            StoreChunkThread thread = new StoreChunkThread(message);
            Peer.executor.execute(thread);
            return;
        }

        // Message reached initiator peer, don't store chunk and forward it to successor if possible
        message.forwardToSuccessor(false);
    }

    private void handleStoredMessage(StoredMessage message) {
        ChunkIdentifier identifier = new ChunkIdentifier(message.fileId, message.chunkNumber);
        Peer.state.chunkReplicationDegreeMap.get(identifier).add(message.senderAddress);
    }

    private void handleDeleteMessage(DeleteMessage message) {
        // Get folder corresponding to file id
        String fileId = message.fileId;
        File folder = new File("peer" + Peer.id + File.separator + fileId);

        if (folder.exists() && folder.isDirectory()) {
            File[] chunkFiles = folder.listFiles();
            if (chunkFiles != null) {
                for (File chunkFile : chunkFiles) {
                    try {
                        int chunkNumber = Integer.parseInt(chunkFile.getName());
                        ChunkIdentifier identifier = new ChunkIdentifier(fileId, chunkNumber);
                        Peer.state.storedChunksMap.remove(identifier);
                        chunkFile.delete();
                    }
                    catch (NumberFormatException ex) {
                        System.out.println("Chunk file has invalid name: " + ex.getMessage());
                    }
                }
            }
            folder.delete();
        }
    }

    private void handleGetChunkMessage(GetChunkMessage message) {
        // TODO
    }

    private void handleChunkMessage(ChunkMessage message) {
        // TODO
    }
}
