import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class BaaS {

    public void runServer(String args) throws IOException {
        System.out.println("** BaaS -> ** I got the data: " + args);
        int port = Integer.parseInt(args.split(";")[5].split(":")[1].split("_")[1]);
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("** BaaS -> ** run... on port " + port);
        LinkedList<String> requests = new LinkedList<>();
        LinkedList<String> responses = new LinkedList<>();
        int serviceInstance = Integer.parseInt(args.split(";")[4].split(":")[1]);
        ArrayList<BaaSThread> threads = new ArrayList<>();
        BaaSToAgentConnectionThread serviceToAgentConnectionThread = new BaaSToAgentConnectionThread(requests,
                responses, args, threads, serviceInstance);
        while (true) {
            Socket socket = serverSocket.accept();

            System.out.println("** BaaS -> ** New client");
            new BaaSThread(socket, serviceToAgentConnectionThread, responses, port, serviceInstance, threads);
        }
    }
}
