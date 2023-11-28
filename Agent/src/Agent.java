import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.LinkedList;

public class Agent {

    public void runServer() throws IOException {
        int startPort = 34011;
        ServerSocket serverSocket = new ServerSocket(startPort);
        LinkedList<ServiceToAgentMessageWithPort> requests = new LinkedList<>();
        HashMap<Integer, ServiceToAgentConnectionThread> servicePorts = new HashMap<Integer, ServiceToAgentConnectionThread>();
        System.out.println("Agent run... on port " + startPort);
        new AgentToManagerConnectionThread(servicePorts, requests, startPort);

        // AgentToManagerConnectionThread agentToManagerConnectionThread = new
        // AgentToManagerConnectionThread(
        // servicePorts, requests);
        while (true) {
            Socket socket = serverSocket.accept();
            new ServiceToAgentConnectionThread(socket, servicePorts, requests);
        }
    }
}
