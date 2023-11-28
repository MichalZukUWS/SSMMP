import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class ApiGateway {
    public ServerSocket serverSocket;

    public void runServer() throws IOException {
        int startPort = 34100;
        serverSocket = new ServerSocket(startPort);
        System.out.println("Api Gateway run... on port " + startPort);
        LinkedList<String> requests = new LinkedList<>();
        LinkedList<String> responses = new LinkedList<>();
        ApiGatewayToAgentConnectionThread serviceToAgentConnectionThread = new ApiGatewayToAgentConnectionThread(
                requests,
                responses);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Api Gateway -> New Connection");
            new ApiGatewayThread(socket, startPort, serviceToAgentConnectionThread, responses);
        }
    }
}
