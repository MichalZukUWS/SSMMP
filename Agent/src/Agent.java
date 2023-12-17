import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Agent {

    public void runServer() throws IOException {
        int startPort = 34011;
        ServerSocket serverSocket = new ServerSocket(startPort);
        LinkedList<ServiceToAgentMessageWithPort> requests = new LinkedList<>();
        HashMap<Integer, AgentToServiceConnectionThread> servicePorts = new HashMap<Integer, AgentToServiceConnectionThread>();
        System.out.println("-- Agent -> -- run... on port " + startPort);
        ArrayList<ServiceConnectionWithAgentEntry> serivcePorts = new ArrayList<>();
        AgentToManagerConnectionThread agentToManagerConnectionThread = new AgentToManagerConnectionThread(servicePorts,
                requests, startPort, serivcePorts);
        new AgentCheckServiceHealthThread(serivcePorts);
        while (true) {
            Socket socket = serverSocket.accept();
            new AgentToServiceConnectionThread(socket, servicePorts, agentToManagerConnectionThread, serivcePorts);
        }
    }
}
