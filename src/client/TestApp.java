package client;

import chord.ChordNode;
import protocol.ChunkIdentifier;
import protocol.ChunkInformation;
import protocol.FileInformation;
import protocol.PeerState;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.Map;
import java.util.Set;

public class TestApp {
    public static void printState(PeerState state) {
        if (state.maxDiskSpace != null) {
            double maxDiskSpaceKB = (double) state.maxDiskSpace / 1000.0;
            System.out.println("Maximum disk space: " + maxDiskSpaceKB + " KBytes");
        }

        double spaceOccupiedKB = (double) state.getSpaceOccupied() / 1000.0;
        System.out.println("Space occupied: " + spaceOccupiedKB + " KBytes");

        System.out.println("Files whose backup this peer initiated:");

        for (Map.Entry<String, FileInformation> entry : state.backupFilesMap.entrySet()) {
            FileInformation value = entry.getValue();

            System.out.println(entry.getKey());
            System.out.println("\tID: " + value.fileId);
            System.out.println("\tDesired replication degree: " + value.desiredReplicationDegree);

            for (Map.Entry<ChunkIdentifier, Set<InetSocketAddress>> chunkEntry : state.chunkReplicationDegreeMap.entrySet()) {
                if (value.fileId.equals(chunkEntry.getKey().fileId)) {
                    System.out.println("\tChunk " + chunkEntry.getKey().chunkNumber);
                    System.out.println("\t\tPerceived replication degree: " + chunkEntry.getValue().size());
                }
            }

            System.out.println();
        }

        System.out.println("Chunks this peer is backing up:");

        for (Map.Entry<ChunkIdentifier, ChunkInformation> entry : state.storedChunksMap.entrySet()) {
            ChunkIdentifier key = entry.getKey();
            ChunkInformation value = entry.getValue();

            double sizeKB = (double) value.size / 1000.0;

            System.out.println("File " + key.fileId + " | Chunk " + key.chunkNumber);
            System.out.println("\tSize: " + sizeKB + " KBytes");
            System.out.println("\tDesired replication degree: " + value.desiredReplicationDegree);

            System.out.println();
        }

        System.out.println("Chord protocol information:");

        ChordNode chordNode = state.chordNode;
        System.out.println("\t- Self: " + chordNode.selfInfo);
        System.out.println("\t- Predecessor: " + chordNode.predecessorInfo);

        for (int i = 0; i < ChordNode.keyBits; ++i) {
            long startKey = (chordNode.selfInfo.id + (long) Math.pow(2, i)) % ChordNode.maxNodes;
            long endKey = (startKey + (long) Math.pow(2, i) - 1) % ChordNode.maxNodes;

            System.out.print("\t- Finger[" + i + "]");
            if (i == 0) System.out.print(" (successor)");
            System.out.print(" [" + startKey + ", " + endKey + "]");
            System.out.println(": " + chordNode.fingerTable.get(i));
        }
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) {
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            System.out.println("To specify a host different than localhost for <peer_ap> use the format <host>/<object_name>");
            return;
        }

        try {
            String host = null, objectName = args[0];
            String[] accessPointComponents = args[0].split("/");

            if (accessPointComponents.length == 2) {
                host = accessPointComponents[0];
                objectName = accessPointComponents[1];
            }

            Registry registry = LocateRegistry.getRegistry(host);
            ClientInterface stub = (ClientInterface) registry.lookup(objectName);

            switch (args[1]) {
                case "BACKUP":
                    if (args.length == 4) {
                        stub.backup(args[2], Integer.parseInt(args[3]));
                    }
                    break;
                case "RESTORE":
                    if (args.length == 3) {
                        stub.restore(args[2]);
                    }
                    break;
                case "DELETE":
                    if (args.length == 3) {
                        stub.delete(args[2]);
                    }
                    break;
                case "RECLAIM":
                    if (args.length == 3) {
                        stub.reclaim(Long.parseLong(args[2]));
                    }
                    break;
                case "STATE":
                    printState(stub.state());
                    break;
                default:
                    System.out.println("Error: operation " + args[1] + " is not supported.");
                    break;
            }
        }
        catch (Exception ex) {
            System.err.println("Client exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
