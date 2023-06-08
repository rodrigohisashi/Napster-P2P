import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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

        ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
        LocateRegistry.createRegistry(registryPortNumber);
        Registry registry = LocateRegistry.getRegistry(registryIP, registryPortNumber);
        registry.rebind("server", stub);
        System.out.println("Servidor INICIADO");
    }

    @Override
    public String join(String nomePeer, List<String> arquivos) throws RemoteException {
        PeerInf novoPeer = new PeerInf(nomePeer, arquivos);
        peers.put(nomePeer, novoPeer);
        return "JOIN_OK";
    }

    @Override
    public List<PeerInf> search(String filename) throws RemoteException {
        List<PeerInf> peersWithFile = new ArrayList<>();
        for (PeerInf peer : peers.values()) {
            if (peer.getArquivos().contains(filename)) {
                peersWithFile.add(peer);
            }
        }
        return peersWithFile;
    }

    @Override
    public String update(String peerName, String filename) throws RemoteException {
        if (peers.containsKey(peerName)) {
            PeerInf peerCadastrado = peers.get(peerName);
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
        private String nomePeer;
        private List<String> arquivos;

        public PeerInf(String nomePeer, List<String> arquivos) {
            this.nomePeer = nomePeer;
            this.arquivos = arquivos;
        }

        public String getNomePeer() {
            return nomePeer;
        }

        public List<String> getArquivos() {
            return arquivos;
        }

        public void addFile(String filename) {
            arquivos.add(filename);
        }
    }
}
