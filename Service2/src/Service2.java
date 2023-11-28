import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Service2 {
    public ServerSocket serverSocket;

    public void runServer(String args) throws IOException {
        System.out.println("Service2 -> At the start I got the data: " + args);
        int port = Integer.parseInt(args.split(";")[5].split(":")[1].split("_")[1]);
        serverSocket = new ServerSocket(port);
        System.out.println("Service2 run... on port " + port);
        LinkedList<String> requests = new LinkedList<>();
        LinkedList<String> responses = new LinkedList<>();
        int serviceInstance = Integer.parseInt(args.split(";")[4].split(":")[1]);
        Service2ToAgentConnectionThread serviceToAgentConnectionThread = new Service2ToAgentConnectionThread(requests,
                responses, args);
        while (true) {
            Socket socket = serverSocket.accept();

            System.out.println("Service2 -> New connection");
            new Service2Thread(socket, serviceToAgentConnectionThread, responses, port, serviceInstance);

        }
    }
}
