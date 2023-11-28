import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestServer extends Thread {
    private ServerSocket serverSocket;
    private boolean runThread;

    public TestServer() throws IOException {
        serverSocket = new ServerSocket(15000);
        runThread = true;
        start();
    }

    public void run() {
        while (runThread) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Socket localPort: " + socket.getLocalPort());
                System.out.println("Socket port: " + socket.getPort());
                socket.close();
                runThread = false;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
