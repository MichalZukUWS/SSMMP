import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class ServiceThread extends Thread {

    private Socket socketToApiGateway;
    private PrintWriter writerToApiGateway;
    private BufferedReader readerFromApiGateway;
    private ServiceToAgentConnectionThread serviceToAgentConnectionThread;
    private LinkedList<String> responses;
    private Socket socketToService;
    private PrintWriter writerToService;
    private BufferedReader readerFromService;
    private ArrayList<ServiceThread> threads;
    private int baasPort;
    private boolean send;

    private int myPort;
    private int serviceInstance;
    private String typeOfService;

    public ServiceThread(Socket socket, ServiceToAgentConnectionThread serviceToAgentConnectionThread,
            LinkedList<String> responses, int myPort, int serviceInstance, ArrayList<ServiceThread> threads,
            String typeOfService)
            throws IOException {
        this.socketToApiGateway = socket;
        this.responses = responses;
        this.serviceInstance = serviceInstance;
        this.serviceToAgentConnectionThread = serviceToAgentConnectionThread;
        writerToApiGateway = new PrintWriter(this.socketToApiGateway.getOutputStream());
        readerFromApiGateway = new BufferedReader(new InputStreamReader(this.socketToApiGateway.getInputStream()));
        this.myPort = myPort;
        this.threads = threads;
        this.typeOfService = typeOfService;
        this.threads.add(this);
        start();
    }

    private String[] deconvertFromProtocole(String data) {
        return data.split(";");
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                send = true;
                // receiving and modifying data
                String data = readerFromApiGateway.readLine();
                System.out.println(typeOfService + " -> Received data from Api Gateway: " + data);
                String[] decodedData = deconvertFromProtocole(data);
                serviceToAgentConnectionThread.addRequestToAgent(
                        "type:process_data;message_id:" + decodedData[1].split(":")[1]
                                + ";sub_type:Service_to_agent;source_name:" + typeOfService + ";service_instance_id:"
                                + serviceInstance + "");

                switch (typeOfService) {
                    case "Register":
                        String registerPassword = decodedData[3].split(":")[1];
                        if (registerPassword.length() < 8) {
                            send = false;
                            data = "type:register_response;message_id:" + decodedData[1].split(":")[1]
                                    + ";status:300;data:Your password needs to be at least 8 charakters long.";
                        }
                        break;
                    case "Login":
                        String loginPassword = decodedData[3].split(":")[1];
                        if (loginPassword.length() < 8) {
                            send = false;
                            data = "type:login_response;message_id:" + decodedData[1].split(":")[1]
                                    + ";status:300;data:Your password needs to be at least 8 charakters long.";
                        }
                        break;
                    // TODO: add validation of other services?
                    default:
                        break;
                }

                if (send) {
                    String toSend = null;
                    if (socketToService == null) {
                        // send request for the port
                        toSend = "type:session_request;" + decodedData[1]
                                + ";sub_type:service_to_agent;source_service_name:" + typeOfService
                                + ";source_service_instance_id:"
                                + serviceInstance + ";source_plug_name:P;dest_service_name:BaaS;dest_socket_name:S";
                        System.out.println(typeOfService + " -> Adds to the queue of requests to the Agent: " + toSend);

                        serviceToAgentConnectionThread.addRequestToAgent(toSend);

                        String responseFromAgent = null;
                        synchronized (responses) {
                            while (responses.size() == 0) {
                                try {
                                    responses.wait();
                                } catch (InterruptedException e) {
                                    System.out.println(typeOfService + " Exception: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                            responseFromAgent = responses.poll();
                            responses.notify();
                        }

                        System.out.println(
                                typeOfService + " -> Received data from Agent from the queue: " + responseFromAgent);

                        baasPort = Integer.parseInt(responseFromAgent.split(";")[5].split(":")[1]);

                        socketToService = new Socket("localhost", baasPort);
                        writerToService = new PrintWriter(socketToService.getOutputStream());
                        readerFromService = new BufferedReader(new InputStreamReader(socketToService.getInputStream()));

                        System.out.println(typeOfService + " -> Established a connection to BaaS");

                        toSend = "type:session_ack;" + decodedData[1]
                                + ";sub_type:service_to_agent;status:200;source_plug_port:" + myPort
                                + ";dest_socket_new_port:"
                                + baasPort;

                        System.out.println(typeOfService + " -> Sending data to the agent: " + toSend);

                        serviceToAgentConnectionThread.addRequestToAgent(toSend);
                    }

                    System.out.println(typeOfService + " -> Sending data to Baas: " + data);

                    writerToService.println(data);
                    writerToService.flush();

                    String responseFromService = readerFromService.readLine();

                    System.out.println(typeOfService
                            + " -> Received data from BaaS, and sending response to Api Gateway: "
                            + responseFromService);

                    writerToApiGateway.println(responseFromService);
                    writerToApiGateway.flush();
                } else {
                    writerToApiGateway.println(data);
                    writerToApiGateway.flush();
                }

            }

        } catch (IOException e) {
            System.out.println(typeOfService + " Exception: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println(typeOfService + " -> Handle the closing procedure.");
        writerToApiGateway.close();
        try {
            readerFromApiGateway.close();
            socketToApiGateway.close();
        } catch (IOException e) {
            System.out.println(typeOfService + " Exception: " + e.getMessage());
            e.printStackTrace();
        }

        // Api Gateway -> source, Service -> dest
        // TODO: what about message_id?
        // TODO: 123 -> Api Gateway port
        String dataToAgent = "type:dest_service_session_close_info;message_id:" + 10
                + ";sub_type:dest_service_to_agent;source_service_instance_network_address:localhost_" + 123
                + ";source_plug_name:P;source_plug_port:" + 123
                + ";dest_service_name:" + typeOfService + ";dest_service_instance_network_address:localhost_" + myPort
                + ";dest_service_instance_id:" + serviceInstance + ";dest_socket_name:S;dest_socket_port:" + myPort
                + ";dest_socket_new_port:l";
        serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
        if (socketToService != null) {
            writerToService.close();
            try {
                readerFromService.close();
                socketToService.close();
            } catch (IOException e) {
                System.out.println(typeOfService + " Exception: " + e.getMessage());
                e.printStackTrace();
            }

            // Service -> source, BaaS -> dest
            // TODO: what about message_id?
            dataToAgent = "type:source_service_session_close_info;message_id"
                    + 10
                    + ";sub_type:source_service_to_agent;source_service_name:" + typeOfService
                    + ";source_service_instance_network_address:localhost_"
                    + myPort
                    + ";source_service_instance_id:100;source_plug_name:P;source_plug_port:"
                    + myPort
                    + ";dest_service_name:BaaS;dest_service_instance_network_address:localhost_" + myPort
                    + ";dest_socket_name:S;dest_socket_port:" + baasPort
                    + ";dest_socket_new_port:l";

            serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
        }

    }
}
