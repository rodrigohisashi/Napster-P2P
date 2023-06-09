import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.List;

public interface ServerInterface extends Remote {
    String join(String nomePeer, String ipPeer, int portPeer, List<String> arquivos) throws RemoteException, ServerNotActiveException;

    List<String> search(String filename) throws RemoteException;

    String update(String peerName, String filename) throws RemoteException;
}
