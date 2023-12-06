import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        ApiGatewayToAgentConnectionThread serviceToAgentConnectionThread = new ApiGatewayToAgentConnectionThread(
                requests,
                responses);
        HashMap<String, ApiGatewayToServiceConnectionThread> threadsWithServices = new HashMap<>();
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Api Gateway -> New Connection");
            new ApiGatewayThread(socket, startPort, serviceToAgentConnectionThread, responses, threadsWithServices);
            // BufferedReader readerFromService = new BufferedReader(new
            // InputStreamReader(socket.getInputStream()));
            // String dataFromService = readerFromService.readLine();
            // if
            // (threadsWithServices.keySet().contains(dataFromService.split(";")[1].split(":")[1]))
            // {
            // readerFromService.close();
            // //
            // threadsWithServices.get(dataFromService.split(";")[1].split(":")[1]).foo();
            // } else {
            // readerFromService.close();
            // new ApiGatewayThread(socket, startPort, serviceToAgentConnectionThread,
            // responses);
            // }
        }
    }
}
