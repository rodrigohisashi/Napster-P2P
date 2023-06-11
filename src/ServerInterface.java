import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.List;

public interface ServerInterface extends Remote {
    String join(String ipPeer, int portPeer, List<String> arquivos) throws RemoteException, ServerNotActiveException;

    List<String> search(String peerAdress, String filename) throws RemoteException, ServerNotActiveException;

    String update(String peerAdress, String filename) throws RemoteException;
}
