import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerInterface extends Remote {
    String join(String peerName, List<String> files) throws RemoteException;

    List<Servidor.PeerInf> search(String filename) throws RemoteException;

    String update(String peerName, String filename) throws RemoteException;
}
