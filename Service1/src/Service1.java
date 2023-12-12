import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class Service1 {
    public ServerSocket serverSocket;

    public void runServer(String args) throws IOException {
        System.out.println("Service1 -> At the start I got the data: " + args);
        // 0 -> type:execution_request;
        // 1 -> message_id:1000;
        // 2 -> agent_newtork_address:localhost_34020;
        // 3 -> service_name:Service1;
        // 4 -> service_instance:1;
        // 5 -> socket_configuration:localhost_34022;
        // 6 -> plug_configuration:configuration of plugs
        int port = Integer.parseInt(args.split(";")[5].split(":")[1].split("_")[1]);
        serverSocket = new ServerSocket(port);
        System.out.println("Service1 run... on port " + port);
        LinkedList<String> requests = new LinkedList<>();
        LinkedList<String> responses = new LinkedList<>();
        int serviceInstance = Integer.parseInt(args.split(";")[4].split(":")[1]);
        ArrayList<Service1Thread> threads = new ArrayList<>();
        Service1ToAgentConnectionThread serviceToAgentConnectionThread = new Service1ToAgentConnectionThread(requests,
                responses, args, threads);
        while (true) {
            Socket socket = serverSocket.accept();

            System.out.println("Service1 -> New connection");
            new Service1Thread(socket, serviceToAgentConnectionThread, responses, port,
                    serviceInstance, threads);
        }
    }
}
