import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Manager {

    public void runServer() throws IOException {
        int startPort = 34010;
        ServerSocket serverSocket = new ServerSocket(startPort);
        System.out.println("Manager port: " + startPort);
        while (true) {
            Socket socket = serverSocket.accept();
            new AgentToManagerConnectionThread(socket, startPort + 25);
        }
    }
}
