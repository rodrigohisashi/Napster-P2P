import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Servidor implements ServerInterface {

    public static final int REGISTRY_PORT_NUMBER_DEFAULT = 1099;
    public static final String REGISTRY_IP_DEFAULT = "127.0.0.1";
    private Map<String, PeerInf> peers = new HashMap();

    public static void main(String[] args) {
        try {
            iniciarServidor();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void iniciarServidor() throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Insira o IP do registry: ");

        String registryIP = scanner.nextLine().trim();

        if (registryIP.isEmpty()) {
            registryIP = REGISTRY_IP_DEFAULT;
        }
        System.out.print("Insira a porta do registry: ");
        String registryPort = scanner.nextLine().trim();
        int registryPortNumber;
        if (registryPort.isEmpty()) {
            registryPortNumber = REGISTRY_PORT_NUMBER_DEFAULT;
        } else {
            registryPortNumber = Integer.parseInt(registryPort);
        }
        Servidor server = new Servidor();

        // Criação do Servidor + Registry
        ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
        LocateRegistry.createRegistry(registryPortNumber);
        Registry registry = LocateRegistry.getRegistry(registryIP, registryPortNumber);
        registry.rebind("server", stub);
    }

    @Override
    public String join(String ipPeer, int portPeer, List<String> arquivos) throws RemoteException {
        String ipAndPortPeer = ipPeer + ':' + portPeer;
        if (peers.containsKey(ipAndPortPeer)) {
            return "JOIN_ERROR: Peer já cadastrado";
        }
        PeerInf novoPeer = new PeerInf(ipPeer, portPeer, arquivos);
        peers.put(ipAndPortPeer, novoPeer);
        System.out.println("Peer " + ipPeer + ":" + portPeer + " adicionado com arquivos " + arquivos.toString());
        return "JOIN_OK";
    }

    @Override
    public List<String> search(String peerAdress, String filename) throws RemoteException {
        List<String> ipsPeersWithFile = new ArrayList<>();
        for (PeerInf peer : peers.values()) {
            if (peer.getArquivos().contains(filename)) {
                ipsPeersWithFile.add(peer.peerIp + ":" + peer.peerPort);
            }
        }

        System.out.println("Peer " + peerAdress + " solicitou arquivo " + filename);

        return ipsPeersWithFile;
    }

    @Override
    public String update(String peerAdress, String filename) throws RemoteException {
        if (peers.containsKey(peerAdress)) {
            PeerInf peerCadastrado = peers.get(peerAdress);
            if (peerCadastrado.getArquivos().contains(filename)) {
                return "UPDATE ERROR: FILENAME ALREADY EXISTS!";
            } else {
                peerCadastrado.addFile(filename);
                return "UPDATE_OK";
            }
        } else {
            return "UPDATE ERROR: PEER NOT FOUND";
        }
    }

    public class PeerInf implements Serializable {
        private String peerIp;
        private int peerPort;
        private List<String> arquivos;

        public PeerInf(String peerIp, int peerPort, List<String> arquivos) {
            this.arquivos = arquivos;
            this.peerIp = peerIp;
            this.peerPort = peerPort;
        }

        public List<String> getArquivos() {
            return arquivos;
        }

        public void addFile(String filename) {
            arquivos.add(filename);
        }
    }
}
