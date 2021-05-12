package client;

import protocol.PeerState;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote {
    void backup(String filePath, int replicationDegree) throws RemoteException;
    void restore(String filePath) throws RemoteException;
    void delete(String filePath) throws RemoteException;
    void reclaim(long diskSpace) throws RemoteException;
    PeerState state() throws RemoteException;
}
