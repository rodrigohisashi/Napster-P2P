import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;

public class PeerClient {

    private String name;
    private String serverIp;
    private int serverPort;
    private String folderName;

    public PeerClient(String name, String serverIp, int serverPort, String folderName) {
        this.name = name;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.folderName = folderName;
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Insira o IP: ");
            String serverIp = scanner.nextLine().trim();
            if (serverIp.isEmpty()) {
                serverIp = Servidor.REGISTRY_IP_DEFAULT;
            }

            System.out.print("Insira a porta: ");
            String serverPortInput = scanner.nextLine().trim();
            int serverPort;
            if (serverPortInput.isEmpty()) {
                serverPort = 1099;
            } else {
                serverPort = Integer.parseInt(serverPortInput);
            }

            System.out.print("Insira a pasta: ");
            String folderName = scanner.nextLine().trim();

            System.out.print("Insira o nome do peer: ");
            String name = scanner.nextLine().trim();

            PeerClient peerClient = new PeerClient(name, serverIp, serverPort, folderName);

            Registry registry = LocateRegistry.getRegistry(peerClient.serverIp, peerClient.serverPort);
            ServerInterface servidor = (ServerInterface) registry.lookup("server");

            String joinResponse = servidor.join(peerClient.name, new ArrayList<>());

            if (joinResponse.equals("JOIN_OK")) {
                System.out.println("Peer conectado ao servidor com sucesso.");
            } else {
                System.out.println("Erro ao conectar ao servidor: " + joinResponse);
                System.exit(1);
            }

            System.out.println("Peer iniciado e aguardando requisições...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
