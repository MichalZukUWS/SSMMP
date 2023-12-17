import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;

public class ApiGatewayToServiceConnectionThread extends Thread {

    private Socket socketFromClient;
    private PrintWriter writerToClient;

    private Socket socketToService;
    private PrintWriter writerToService;
    private BufferedReader readerFromService;

    private LinkedList<String> requests;
    private ApiGatewayToAgentConnectionThread apiGatewayToAgentConnectionThread;

    private HashMap<String, ApiGatewayToServiceConnectionThread> threadsWithServices;
    private LinkedList<String> responses;
    private String nameOfService;

    private String firstData;
    private int apiGatewayPort;
    private int servicePort;
    private Thread checkConnectionWithService;

    public ApiGatewayToServiceConnectionThread(Socket socketFromClient,
            ApiGatewayToAgentConnectionThread apiGatewayToAgentConnectionThread,
            HashMap<String, ApiGatewayToServiceConnectionThread> threadsWithServices, LinkedList<String> responses,
            String firstData, int apiGatewayPort) {

        this.socketFromClient = socketFromClient;
        this.apiGatewayToAgentConnectionThread = apiGatewayToAgentConnectionThread;
        this.threadsWithServices = threadsWithServices;
        this.responses = responses;
        this.firstData = firstData;
        this.apiGatewayPort = apiGatewayPort;
        requests = new LinkedList<>();
        start();

    }

    // TODO: connect interrupt with closeConnectionWithService?
    @Override
    public void interrupt() {
        System.out.println("Api Gateway -> Process closing connection with Service.");
        // sending information about closing connection

        String dataToAgent = "type:source_service_session_close_info;message_id:"
                + 1
                + ";sub_type:source_service_to_agent;source_service_name:Api Gateway;source_service_instance_network_address:localhost_"
                + apiGatewayPort
                + ";source_service_instance_id:1;source_plug_name:P;source_plug_port:"
                + apiGatewayPort
                + ";dest_service_name:" + nameOfService
                + ";dest_service_instance_network_address:localhost_" + servicePort
                + ";dest_socket_name:S;dest_socket_port:" + servicePort
                + ";dest_socket_new_port:l";

        apiGatewayToAgentConnectionThread.addRequestToAgent(dataToAgent);

        threadsWithServices.remove(nameOfService);
    }

    public void send(Socket socketFromClient, String data) throws IOException {
        // TODO:: change to list with objects which contains data and socket to client, in this state posisible problems with high demand?

        // if request is from diffrent client have to change socket and writer to client
        if (this.socketFromClient != socketFromClient) {
            this.socketFromClient = socketFromClient;
            writerToClient = new PrintWriter(this.socketFromClient.getOutputStream());

        }
        // adding request to requests list
        synchronized (requests) {
            requests.add(data);
            requests.notify();
        }
    }

    public void closeConnectionWithService(String message) {
        System.out.println("Api Gateway -> Closing connection with: " + nameOfService + ".");
        writerToService.close();
        try {
            readerFromService.close();
            socketToService.close();
        } catch (IOException e) {
            System.out.println("Api Gateway Exception: " + e.getMessage());
        }

        // TODO: add some cheking maybe in catch thread?
        String dataToAgent = "type:source_service_session_close_response;message_id:"
                + message.split(";")[1].split(":")[1] + ";sub_type:source_Service_to_agent;status:200";
        apiGatewayToAgentConnectionThread.addRequestToAgent(dataToAgent);

        dataToAgent = "type:source_service_session_close_info;message_id:"
                + 1
                + ";sub_type:source_service_to_agent;source_service_name:Api Gateway;source_service_instance_network_address:localhost_"
                + apiGatewayPort
                + ";source_service_instance_id:1;source_plug_name:P;source_plug_port:"
                + apiGatewayPort
                + ";dest_service_name:" + nameOfService
                + ";dest_service_instance_network_address:localhost_" + servicePort
                + ";dest_socket_name:S;dest_socket_port:" + servicePort
                + ";dest_socket_new_port:l";

        apiGatewayToAgentConnectionThread.addRequestToAgent(dataToAgent);
    }

    @Override
    public void run() {
        try {
            // Establishing of connection to service

            // request for informations about destination service
            String typeOfService;
            switch (firstData.split(";")[0].split(":")[1]) {
                case "register_request":
                    typeOfService = "Register";
                    break;
                case "login_request":
                    typeOfService = "Login";
                    break;
                case "display_posts_request":
                    typeOfService = "Post";
                    break;
                case "chat_request":
                    typeOfService = "Chat";
                    break;
                case "file_upload_request":
                    typeOfService = "File";
                    break;
                case "file_download_request":
                    typeOfService = "File";
                    break;
                default:
                    typeOfService = null;
                    break;
            }
            String dataToAgent = "type:session_request;message_id:" + firstData.split(";")[1].split(":")[1]
                    + ";sub_type:service_to_agent;source_service_name:Api Gateway;source_service_instance_id:100;source_plug_name:P;dest_service_name:";

            dataToAgent += typeOfService;

            nameOfService = typeOfService;

            dataToAgent += ";dest_socket_name:S";
            System.out.println("Api Gateway -> Data transferred to the agent:" + dataToAgent);
            apiGatewayToAgentConnectionThread.addRequestToAgent(dataToAgent);
            String response = null;

            // waiting for response
            synchronized (responses) {
                while (responses.size() == 0) {
                    try {
                        responses.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Api Gateway Exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                response = responses.poll();
            }
            System.out.println("Api Gateway -> From the queue I received:" + response);
            servicePort = Integer.parseInt(response.split(";")[5].split(":")[1]);
            // Establishing connection with destination service
            socketToService = new Socket("localhost", servicePort);
            writerToService = new PrintWriter(socketToService.getOutputStream());
            readerFromService = new BufferedReader(new InputStreamReader(socketToService.getInputStream()));
            System.out.println("Api Gateway -> Established a connection to Service");

            // sending information about establishing connection
            dataToAgent = "type:session_ack;message_id:" + firstData.split(";")[1].split(":")[1]
                    + ";sub_type:service_to_agent;status:200;source_plug_port:34100;dest_socket_new_port:"
                    + servicePort;

            System.out
                    .println("Api Gateway -> Data to the Agent added to the queue of requests: "
                            + dataToAgent);

            apiGatewayToAgentConnectionThread.addRequestToAgent(dataToAgent);

            // putting service name and thread with connection with connection to specific
            // service into map
            synchronized (threadsWithServices) {

                threadsWithServices.put(firstData.split(";")[0].split(":")[1], this);
            }
            // creating writer to client
            writerToClient = new PrintWriter(socketFromClient.getOutputStream());
            // adding first request to list
            synchronized (requests) {
                requests.add(firstData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        checkConnectionWithService = new Thread(() -> {
            try {
                while (!isInterrupted()) {
                    synchronized (readerFromService) {
                        readerFromService.mark(1);
                        readerFromService.read();
                        readerFromService.reset();
                        readerFromService.notify();

                    }
                }
            } catch (IOException e) {
                // System.out.println(
                // "Api Gateway Exception in thread which is responsible for checking connection
                // to Service: "
                // + e.getMessage());
                // e.printStackTrace();
                checkConnectionWithService.interrupt();
                interrupt();
            }
            System.out.println("Api Gateway -> Connection with Service closed.");
        });
        checkConnectionWithService.start();
        // after connection is established thread is pulling from list messages and
        // sends forward to service
        try {
            while (!isInterrupted()) {
                // pulling from list messages and sending forward to service
                synchronized (requests) {
                    if (!requests.isEmpty()) {

                        String data = requests.poll();
                        requests.notify();
                        // sending to service
                        writerToService.println(data);
                        writerToService.flush();
                        // retrieving response from service and sending back to client
                        synchronized (readerFromService) {
                            String dataFromService = readerFromService.readLine();
                            writerToClient.println(dataFromService);
                            writerToClient.flush();
                            readerFromService.notify();
                        }

                    }

                }
            }
        } catch (IOException e) {
            System.out.println(
                    "Api Gateway Exception in thread which is resposible for sending and receiving data from Service: "
                            + e.getMessage());
            // e.printStackTrace();
            checkConnectionWithService.interrupt();
            interrupt();
        }

        // closing writers, readers and sockets
        try {
            if (writerToClient != null)
                writerToClient.close();
            if (socketFromClient != null)
                socketFromClient.close();

            if (writerToService != null)
                writerToService.close();
            if (readerFromService != null)
                readerFromService.close();
            if (socketToService != null)
                socketToService.close();

        } catch (IOException e) {
            System.out.println(
                    "Api Gateway Exception with closing writers, readers and sockets: " + e.getMessage());
            // e.printStackTrace();
        }

        // // sending information about closing connection

        // String dataToAgent = "type:source_service_session_close_info;message_id:"
        // + 1
        // + ";sub_type:source_service_to_agent;source_service_name:Api
        // Gateway;source_service_instance_network_address:localhost_"
        // + apiGatewayPort
        // + ";source_service_instance_id:100;source_plug_name:P;source_plug_port:"
        // + apiGatewayPort
        // + ";dest_service_name:" + nameOfService
        // + ";dest_service_instance_network_address:localhost_" + servicePort
        // + ";dest_socket_name:S;dest_socket_port:" + servicePort
        // + ";dest_socket_new_port:l";

        // apiGatewayToAgentConnectionThread.addRequestToAgent(dataToAgent);

        // threadsWithServices.remove(nameOfService);
    }
}
