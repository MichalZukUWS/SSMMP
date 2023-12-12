import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.stream.Stream;

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
                PrintWriter w = new PrintWriter(socket.getOutputStream());
                w.println("foo");
                w.flush();
                // BufferedReader reader = new BufferedReader(new
                // InputStreamReader(socket.getInputStream()));
                // while (true) {
                // String data = reader.readLine();
                // if (data == null)
                // break;
                // System.out.println(data);
                // System.out.println("///////////////");
                // // Stream<String> foo = reader.lines();
                // // String data = String.join("", foo.toList());
                // // if (data.equals(""))
                // // break;
                // // System.out.println(data);
                // }

                socket.close();
                serverSocket.close();
                runThread = false;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
