import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;

public class ApiGateway {
    public ServerSocket serverSocket;

    public void runServer() throws IOException {
        int startPort = 34100;
        serverSocket = new ServerSocket(startPort);
        System.out.println("Api Gateway run... on port " + startPort);
        LinkedList<String> requests = new LinkedList<>();
        LinkedList<String> responses = new LinkedList<>();
        HashMap<String, ApiGatewayToServiceConnectionThread> threadsWithServices = new HashMap<>();
        ApiGatewayToAgentConnectionThread serviceToAgentConnectionThread = new ApiGatewayToAgentConnectionThread(
                requests,
                responses, startPort, threadsWithServices);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Api Gateway -> New Connection");
            // after new connection create new Thread to process requests and responses
            new ApiGatewayThread(socket, startPort, serviceToAgentConnectionThread, responses, threadsWithServices);
        }
    }
}
