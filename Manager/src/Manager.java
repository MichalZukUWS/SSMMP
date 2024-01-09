import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Manager {

    public void runServer() throws IOException {
        int startPort = 34010;
        ServerSocket serverSocket = new ServerSocket(startPort);
        System.out.println("Manager port: " + startPort);
        LinkedList<String> requests = new LinkedList<>();
        ServicesConnections connections = new ServicesConnections();
        while (true) {
            // after new connection with agent add new Thread to process requests and
            // responses
            Socket socket = serverSocket.accept();
            new AgentToManagerConnectionThread(socket, startPort + 140, requests, connections);
        }
    }
}
