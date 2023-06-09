import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class PeerClient {

    private String name;
    private String ip;
    private int port;
    private String folderName;
    private List<String> fileNames;
    private String requestedFile;

    public static final int REGISTRY_PORT_NUMBER_DEFAULT = 1099;

    public static final String REGISTRY_IP_DEFAULT = "127.0.0.1";

    public void setRequestedFile(String requestedFile) {
        this.requestedFile = requestedFile;
    }

    public PeerClient(String name, String ip, int port, String folderName, List<String> fileNames) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.folderName = folderName;
        this.fileNames = fileNames;
    }

    public static void main(String[] args) {
        try {
            PeerClient peerClient = createPeerClient();

            handlerNewDownloadRequestThread downloadServer = new handlerNewDownloadRequestThread(peerClient.getPort(), peerClient.getFolderName());
            // Inicia o servidor de download em uma nova thread
            Thread serverThread = new Thread(downloadServer);
            serverThread.start();

            Registry registry = LocateRegistry.getRegistry(REGISTRY_IP_DEFAULT, REGISTRY_PORT_NUMBER_DEFAULT);
            ServerInterface servidor = (ServerInterface) registry.lookup("server");

            adicionarArquivosAoPeer(peerClient);

            Scanner scanner = new Scanner(System.in);
            int valor;
            do {
                System.out.println("---- Menu Peer ----");
                System.out.println("1 - Join");
                System.out.println("2 - Search");
                System.out.println("3 - Download");
                System.out.print("Digite um número (-1 para sair): ");
                valor = scanner.nextInt();
                switch (valor) {
                    case 1:
                        requisicaoJoin(peerClient, servidor);
                        break;
                    case 2:
                        requisicaoSearch(peerClient, servidor);
                        break;
                    case 3:
                        requisicaoDownload(peerClient, servidor);
                        break;
                }
            } while (valor != -1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void requisicaoDownload(PeerClient peerClient, ServerInterface servidor) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Digite o IP do peer de destino: ");
            String peerIp = scanner.nextLine().trim();

            System.out.print("Digite a porta do peer de destino: ");
            int peerPort = scanner.nextInt();

            // Cria uma conexão TCP com o peer de destino
            Socket socket = new Socket(peerIp, peerPort);

            // Envia a requisição de download para o peer de destino
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeUTF(peerClient.getRequestedFile());

            // Prepara para receber o arquivo do peer de destino
            InputStream inputStream = socket.getInputStream();

            String filePath = peerClient.getFolderName() + File.separator + peerClient.getRequestedFile();
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);

            // Recebe o arquivo e escreve no disco
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            // Fecha as conexões e recursos
            fileOutputStream.close();
            inputStream.close();
            dataOutputStream.close();
            outputStream.close();
            socket.close();

            servidor.update(peerClient.name, peerClient.getRequestedFile());

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erro durante o download do arquivo.");
        }
    }

    private static void requisicaoSearch(PeerClient peerClient, ServerInterface servidor) throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o nome do arquivo para procurar: ");
        String fileName = scanner.nextLine().trim();

        List<String> searchResponse = servidor.search(fileName);

        System.out.println("peers com arquivo solicitado: " + searchResponse.toString());
        peerClient.setRequestedFile(fileName);
    }

    private static void requisicaoJoin(PeerClient peerClient, ServerInterface servidor) throws RemoteException, ServerNotActiveException {
        String joinResponse = servidor.join(peerClient.name, peerClient.ip, peerClient.port, peerClient.fileNames);
        if (joinResponse.equals("JOIN_OK")) {
            System.out.println("Sou peer " + peerClient.ip + ":" + peerClient.port + " com arquivos: " + peerClient.fileNames.toString());
        } else {
            System.out.println("Erro ao conectar ao servidor: " + joinResponse);
            System.exit(1);
        }
    }

    private static PeerClient createPeerClient() {
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

        return new PeerClient(name, serverIp, serverPort, folderName, new ArrayList<>());
    }

    public static void adicionarArquivosAoPeer(PeerClient peerClient) {
        File pasta = new File(peerClient.folderName);
        File[] arquivos = pasta.listFiles();

        if (arquivos != null) {
            for (File arquivo : arquivos) {
                if (arquivo.isFile()) {
                    peerClient.fileNames.add(arquivo.getName());
                }
            }
        }
        System.out.println(Arrays.toString(arquivos));
    }


    private static class handlerNewDownloadRequestThread implements Runnable {
        private int port;
        private String folderName;

        public handlerNewDownloadRequestThread(int port, String folderName) {
            this.port = port;
            this.folderName = folderName;
        }

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Servidor de download iniciado na porta " + port);

                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Nova solicitação de download recebida");

                    Thread downloadThread = new Thread(new DownloadThread(socket, folderName));
                    downloadThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class DownloadThread implements Runnable {
        private Socket socket;
        private String folderName;

        public DownloadThread(Socket socket, String folderName) {
            this.socket = socket;
            this.folderName = folderName;
        }

        @Override
        public void run() {
            try {
                // Recebe o nome do arquivo solicitado
                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                String requestedFile = dataInputStream.readUTF();

                // Abre o arquivo para leitura
                File file = new File(folderName, requestedFile);
                FileInputStream fileInputStream = new FileInputStream(file);

                // Prepara para enviar o arquivo para o peer solicitante
                OutputStream outputStream = socket.getOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // Fecha as conexões e recursos
                fileInputStream.close();
                dataInputStream.close();
                inputStream.close();
                outputStream.close();
                socket.close();


            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Erro durante o download do arquivo.");
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public String getRequestedFile() {
        return requestedFile;
    }

}