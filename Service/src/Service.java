import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class Service {
    public ServerSocket serverSocket;

    public void runServer(String args) throws IOException {
        // 0 -> type:execution_request;
        // 1 -> message_id:1000;
        // 2 -> agent_newtork_address:localhost_34020;
        // 3 -> service_name:Service1;
        // 4 -> service_instance:1;
        // 5 -> socket_configuration:localhost_34022;
        // 6 -> plug_configuration:configuration of plugs
        String typeOfService = args.split(";")[3].split(":")[1];
        int port = Integer.parseInt(args.split(";")[5].split(":")[1].split("_")[1]);
        serverSocket = new ServerSocket(port);
        switch (typeOfService) {
            case "Chat":
                System.out.println("|| " + typeOfService + " -> At the start I got the data: " + args);
                break;

            case "Login":
                System.out.println("__ " + typeOfService + " -> __ At the start I got the data: " + args);
                break;
            case "File":
                System.out.println("++ " + typeOfService + " -> ++ At the start I got the data: " + args);
                break;
            case "Post":
                System.out.println("@@ " + typeOfService + " -> @@ At the start I got the data: " + args);
                break;
            case "Register":
                System.out.println("!! " + typeOfService + " -> !! At the start I got the data: " + args);
                break;
        }
        switch (typeOfService) {
            case "Chat":
                System.out.println("|| " + typeOfService + " -> run... on port " + port);
                break;

            case "Login":
                System.out.println("__ " + typeOfService + " -> __ run... on port " + port);
                break;
            case "File":
                System.out.println("++ " + typeOfService + " -> ++ run... on port " + port);
                break;
            case "Post":
                System.out.println("@@ " + typeOfService + " -> @@ run... on port " + port);
                break;
            case "Register":
                System.out.println("!! " + typeOfService + " -> !! run... on port " + port);
                break;
        }
        LinkedList<String> requests = new LinkedList<>();
        LinkedList<String> responses = new LinkedList<>();
        int serviceInstance = Integer.parseInt(args.split(";")[4].split(":")[1]);
        ArrayList<ServiceThread> threads = new ArrayList<>();
        ServiceToAgentConnectionThread serviceToAgentConnectionThread = new ServiceToAgentConnectionThread(requests,
                responses, args, threads, typeOfService);
        while (true) {
            Socket socket = serverSocket.accept();
            // FIXME: change switch to method
            switch (typeOfService) {
                case "Chat":
                    System.out.println("|| " + typeOfService + " -> || New connection");
                    break;

                case "Login":
                    System.out.println("__ " + typeOfService + " -> __ New connection");
                    break;
                case "File":
                    System.out.println("++ " + typeOfService + " -> ++ New connection");
                    break;
                case "Post":
                    System.out.println("@@ " + typeOfService + " -> @@ New connection");
                    break;
                case "Register":
                    System.out.println("!! " + typeOfService + " -> !! New connection");
                    break;
            }
            new ServiceThread(socket, serviceToAgentConnectionThread, responses, port,
                    serviceInstance, threads, typeOfService);
        }
    }
}
