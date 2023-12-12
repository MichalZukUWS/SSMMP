import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;

public class ApiGatewayThread extends Thread {

    private Socket socketFromClient;
    private PrintWriter writerToClient;
    private BufferedReader readerFromClient;
    private ApiGatewayToAgentConnectionThread serviceToAgentConnectionThread;
    private LinkedList<String> responses;
    private int apiGatewayPort;
    private HashMap<String, ApiGatewayToServiceConnectionThread> threadsWithServices;

    public ApiGatewayThread(
            Socket socketFromClient, int startPort, ApiGatewayToAgentConnectionThread serviceToAgentConnectionThread,
            LinkedList<String> responses, HashMap<String, ApiGatewayToServiceConnectionThread> threadsWithServices)
            throws IOException {
        this.socketFromClient = socketFromClient;
        writerToClient = new PrintWriter(socketFromClient.getOutputStream());
        readerFromClient = new BufferedReader(new InputStreamReader(socketFromClient.getInputStream()));
        this.serviceToAgentConnectionThread = serviceToAgentConnectionThread;
        this.responses = responses;
        apiGatewayPort = startPort;
        this.threadsWithServices = threadsWithServices;
        start();
    }

    public void foo() {

    }

    private String[] deconvertFromProtocole(String data) throws NullPointerException {
        return data.split(";");
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {

                String data = readerFromClient.readLine();
                String[] decodedData = deconvertFromProtocole(data);
                // checking if is in map thread with connection with service from request
                synchronized (threadsWithServices) {
                    // if is in map add request to requests list
                    if (threadsWithServices.keySet().contains(decodedData[0].split(":")[1])) {
                        threadsWithServices.get(decodedData[0].split(":")[1]).send(socketFromClient, data);
                        threadsWithServices.notify();
                    }
                    // if isn't create thread with connection to specific service and give request
                    // to process
                    else {
                        new ApiGatewayToServiceConnectionThread(socketFromClient, serviceToAgentConnectionThread,
                                threadsWithServices, responses, data, apiGatewayPort);
                    }
                }

            }
        } catch (IOException e) {
            System.out.println(
                    "Api Gateway Exception in thread which is responsible for reading data from client and sending forward to Service: "
                            + e.getMessage());
            e.printStackTrace();
            interrupt();
        } catch (NullPointerException e) {
            System.out.println(
                    "Api Gateway Exception in thread which is responsible for reading data from client and sending forward to Service: "
                            + e.getMessage());
            e.printStackTrace();
            interrupt();
        }

        System.out.println("Api Gateway -> Client closed connection closing thread.");
        try {
            readerFromClient.close();
            writerToClient.close();
            socketFromClient.close();
        } catch (IOException e) {
            System.out.println(
                    "Api Gateway Exception with closing reader,writer and socket from client: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
