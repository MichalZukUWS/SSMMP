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
        start();
    }

    private String[] deconvertFromProtocole(String data) {
        return data.split(";");
    }

    public void closeConnectionWithBaaS(String message) {
        switch (typeOfService) {
            case "Chat":
                System.out.println("|| " + typeOfService + " -> || Closing connection with BaaS.");
                break;

            case "Login":
                System.out.println("__ " + typeOfService + " -> __ Closing connection with BaaS.");
                break;
            case "File":
                System.out.println("++ " + typeOfService + " -> ++ Closing connection with BaaS.");
                break;
            case "Post":
                System.out.println("@@ " + typeOfService + " -> @@ Closing connection with BaaS.");
                break;
            case "Register":
                System.out.println("!! " + typeOfService + " -> !! Closing connection with BaaS.");
                break;
        }
        if (socketToService != null) {
            writerToService.close();
            try {
                readerFromService.close();
                socketToService.close();
            } catch (IOException e) {
                switch (typeOfService) {
                    case "Chat":
                        System.out.println("|| " + typeOfService + " -> || Exception: " + e.getMessage());
                        break;

                    case "Login":
                        System.out.println("__ " + typeOfService + " -> __ Exception: " + e.getMessage());
                        break;
                    case "File":
                        System.out.println("++ " + typeOfService + " -> ++ Exception: " + e.getMessage());
                        break;
                    case "Post":
                        System.out.println("@@ " + typeOfService + " -> @@ Exception: " + e.getMessage());
                        break;
                    case "Register":
                        System.out.println("!! " + typeOfService + " -> !! Exception: " + e.getMessage());
                        break;
                }
                e.printStackTrace();
            }

            // TODO: add some cheking maybe in catch thread?
            String dataToAgent = "type:source_service_session_close_response;message_id:"
                    + message.split(";")[1].split(":")[1] + ";sub_type:source_Service_to_agent;status:200";
            serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
            // Service -> source, BaaS -> dest
            dataToAgent = "type:source_service_session_close_info;message_id:"
                    + serviceInstance
                    + ";sub_type:source_service_to_agent;source_service_name:" + typeOfService
                    + ";source_service_instance_network_address:localhost_"
                    + myPort
                    + ";source_service_instance_id:" + serviceInstance +
                    ";source_plug_name:P;source_plug_port:"
                    + myPort
                    + ";dest_service_name:BaaS;dest_service_instance_network_address:localhost_"
                    + myPort
                    + ";dest_socket_name:S;dest_socket_port:" + baasPort
                    + ";dest_socket_new_port:l";

            serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
        }
    }

    @Override
    public void interrupt() {
        switch (typeOfService) {
            case "Chat":
                System.out.println(
                        "|| " + typeOfService + " -> || Handling the closing procedure by closing connections.");
                break;

            case "Login":
                System.out.println(
                        "__ " + typeOfService + " -> __ Handling the closing procedure by closing connections.");
                break;
            case "File":
                System.out.println(
                        "++ " + typeOfService + " -> ++ Handling the closing procedure by closing connections.");
                break;
            case "Post":
                System.out.println(
                        "@@ " + typeOfService + " -> @@ Handling the closing procedure by closing connections.");
                break;
            case "Register":
                System.out.println(
                        "!! " + typeOfService + " -> !! Handling the closing procedure by closing connections.");
                break;
        }
        writerToApiGateway.close();
        try {
            readerFromApiGateway.close();
            socketToApiGateway.close();
        } catch (IOException e) {
            switch (typeOfService) {
                case "Chat":
                    System.out.println("|| " + typeOfService + " -> || Exception: " + e.getMessage());
                    break;

                case "Login":
                    System.out.println("__ " + typeOfService + " -> __ Exception: " + e.getMessage());
                    break;
                case "File":
                    System.out.println("++ " + typeOfService + " -> ++ Exception: " + e.getMessage());
                    break;
                case "Post":
                    System.out.println("@@ " + typeOfService + " -> @@ Exception: " + e.getMessage());
                    break;
                case "Register":
                    System.out.println("!! " + typeOfService + " -> !! Exception: " + e.getMessage());
                    break;
            }
            e.printStackTrace();
        }

        // Api Gateway -> source, Service -> dest
        // TODO: 34100 -> Api Gateway port
        String dataToAgent = "type:dest_service_session_close_info;message_id:" + serviceInstance
                + ";sub_type:dest_service_to_agent;source_service_instance_network_address:localhost_" + 34100
                + ";source_plug_name:P;source_plug_port:" + 34100
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
                switch (typeOfService) {
                    case "Chat":
                        System.out.println("|| " + typeOfService + " -> || Exception: " + e.getMessage());
                        break;
                    case "Login":
                        System.out.println("__ " + typeOfService + " -> __ Exception: " + e.getMessage());
                        break;
                    case "File":
                        System.out.println("++ " + typeOfService + " -> ++ Exception: " + e.getMessage());
                        break;
                    case "Post":
                        System.out.println("@@ " + typeOfService + " -> @@ Exception: " + e.getMessage());
                        break;
                    case "Register":
                        System.out.println("!! " + typeOfService + " -> !! Exception: " + e.getMessage());
                        break;
                }
                e.printStackTrace();
            }

            // Service -> source, BaaS -> dest
            dataToAgent = "type:source_service_session_close_info;message_id:"
                    + serviceInstance
                    + ";sub_type:source_service_to_agent;source_service_name:" + typeOfService
                    + ";source_service_instance_network_address:localhost_"
                    + myPort
                    + ";source_service_instance_id:" + serviceInstance +
                    ";source_plug_name:P;source_plug_port:"
                    + myPort
                    + ";dest_service_name:BaaS;dest_service_instance_network_address:localhost_"
                    + myPort
                    + ";dest_socket_name:S;dest_socket_port:" + baasPort
                    + ";dest_socket_new_port:l";

            serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
        }
    }

    @Override
    public void run() {
        synchronized (threads) {
            threads.add(this);
            threads.notify();
        }
        try {
            while (!isInterrupted()) {
                send = true;
                // receiving and modifying data
                String data = readerFromApiGateway.readLine();
                switch (typeOfService) {
                    case "Chat":
                        System.out.println("|| " + typeOfService + " -> || Received data from Api Gateway: " + data);
                        break;
                    case "Login":
                        System.out.println("__ " + typeOfService + " -> __ Received data from Api Gateway: " + data);
                        break;
                    case "File":
                        System.out.println("++ " + typeOfService + " -> ++ Received data from Api Gateway: " + data);
                        break;
                    case "Post":
                        System.out.println("@@ " + typeOfService + " -> @@ Received data from Api Gateway: " + data);
                        break;
                    case "Register":
                        System.out.println("!! " + typeOfService + " -> !! Received data from Api Gateway: " + data);
                        break;
                }
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
                    case "Chat":
                        String message = decodedData[3].split(":")[1];
                        if (message.length() > 255) {
                            send = false;
                            data = "type:chat_response;message_id:" + decodedData[1].split(":")[1]
                                    + ";status:300;data:Provided to long message.";
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
                        switch (typeOfService) {
                            case "Chat":
                                System.out.println(
                                        "|| " + typeOfService + " -> || Adds to the queue of requests to the Agent: "
                                                + toSend);
                                break;
                            case "Login":
                                System.out.println(
                                        "__ " + typeOfService + " -> __ Adds to the queue of requests to the Agent: "
                                                + toSend);
                                break;
                            case "File":
                                System.out.println(
                                        "++ " + typeOfService + " -> ++ Adds to the queue of requests to the Agent: "
                                                + toSend);
                                break;
                            case "Post":
                                System.out.println(
                                        "@@ " + typeOfService + " -> @@ Adds to the queue of requests to the Agent: "
                                                + toSend);
                                break;
                            case "Register":
                                System.out.println(
                                        "!! " + typeOfService + " -> !! Adds to the queue of requests to the Agent: "
                                                + toSend);
                                break;
                        }

                        serviceToAgentConnectionThread.addRequestToAgent(toSend);

                        String responseFromAgent = null;
                        synchronized (responses) {
                            while (responses.size() == 0) {
                                try {
                                    responses.wait();
                                } catch (InterruptedException e) {
                                    switch (typeOfService) {
                                        case "Chat":
                                            System.out.println(
                                                    "|| " + typeOfService + " -> || Exception: " + e.getMessage());
                                            break;
                                        case "Login":
                                            System.out.println(
                                                    "__ " + typeOfService + " -> __ Exception: " + e.getMessage());
                                            break;
                                        case "File":
                                            System.out.println(
                                                    "++ " + typeOfService + " -> ++ Exception: " + e.getMessage());
                                            break;
                                        case "Post":
                                            System.out.println(
                                                    "@@ " + typeOfService + " -> @@ Exception: " + e.getMessage());
                                            break;
                                        case "Register":
                                            System.out.println(
                                                    "!! " + typeOfService + " -> !! Exception: " + e.getMessage());
                                            break;
                                    }
                                    e.printStackTrace();
                                }
                            }
                            responseFromAgent = responses.poll();
                            responses.notify();
                        }

                        switch (typeOfService) {
                            case "Chat":
                                System.out.println(
                                        "|| " + typeOfService + " -> || Received data from Agent from the queue: "
                                                + responseFromAgent);
                                break;
                            case "Login":
                                System.out.println(
                                        "__ " + typeOfService + " -> __ Received data from Agent from the queue: "
                                                + responseFromAgent);
                                break;
                            case "File":
                                System.out.println(
                                        "++ " + typeOfService + " -> ++ Received data from Agent from the queue: "
                                                + responseFromAgent);
                                break;
                            case "Post":
                                System.out.println(
                                        "@@ " + typeOfService + " -> @@ Received data from Agent from the queue: "
                                                + responseFromAgent);
                                break;
                            case "Register":
                                System.out.println(
                                        "!! " + typeOfService + " -> !! Received data from Agent from the queue: "
                                                + responseFromAgent);
                                break;
                        }
                        baasPort = Integer.parseInt(responseFromAgent.split(";")[5].split(":")[1]);

                        socketToService = new Socket("localhost", baasPort);
                        writerToService = new PrintWriter(socketToService.getOutputStream());
                        readerFromService = new BufferedReader(new InputStreamReader(socketToService.getInputStream()));

                        System.out.println(typeOfService + " -> Established a connection to BaaS");

                        toSend = "type:session_ack;" + decodedData[1]
                                + ";sub_type:service_to_agent;status:200;source_plug_port:" + myPort
                                + ";dest_socket_new_port:"
                                + baasPort;

                        switch (typeOfService) {
                            case "Chat":
                                System.out.println(
                                        "|| " + typeOfService + " -> || Sending data to the agent: " + toSend);
                                break;
                            case "Login":
                                System.out.println(
                                        "__ " + typeOfService + " -> __ Sending data to the agent: " + toSend);
                                break;
                            case "File":
                                System.out.println(
                                        "++ " + typeOfService + " -> ++ Sending data to the agent: " + toSend);
                                break;
                            case "Post":
                                System.out.println(
                                        "@@ " + typeOfService + " -> @@ Sending data to the agent: " + toSend);
                                break;
                            case "Register":
                                System.out.println(
                                        "!! " + typeOfService + " -> !! Sending data to the agent: " + toSend);
                                break;
                        }

                        serviceToAgentConnectionThread.addRequestToAgent(toSend);
                    }

                    switch (typeOfService) {
                        case "Chat":
                            System.out.println(
                                    "|| " + typeOfService + " -> || Sending data to Baas: " + data);
                            break;
                        case "Login":
                            System.out.println(
                                    "__ " + typeOfService + " -> __ Sending data to Baas: " + data);
                            break;
                        case "File":
                            System.out.println(
                                    "++ " + typeOfService + " -> ++ Sending data to Baas: " + data);
                            break;
                        case "Post":
                            System.out.println(
                                    "@@ " + typeOfService + " -> @@ Sending data to Baas: " + data);
                            break;
                        case "Register":
                            System.out.println(
                                    "!! " + typeOfService + " -> !! Sending data to Baas: " + data);
                            break;
                    }

                    writerToService.println(data);
                    writerToService.flush();

                    String responseFromService = readerFromService.readLine();

                    switch (typeOfService) {
                        case "Chat":
                            System.out.println(
                                    "|| " + typeOfService
                                            + " -> || Received data from BaaS, and sending response to Api Gateway: "
                                            + responseFromService);
                            break;
                        case "Login":
                            System.out.println(
                                    "__ " + typeOfService
                                            + " -> __ Received data from BaaS, and sending response to Api Gateway: "
                                            + responseFromService);
                            break;
                        case "File":
                            System.out.println(
                                    "++ " + typeOfService
                                            + " -> ++ Received data from BaaS, and sending response to Api Gateway: "
                                            + responseFromService);
                            break;
                        case "Post":
                            System.out.println(
                                    "@@ " + typeOfService
                                            + " -> @@ Received data from BaaS, and sending response to Api Gateway: "
                                            + responseFromService);
                            break;
                        case "Register":
                            System.out.println(
                                    "!! " + typeOfService
                                            + " -> !! Received data from BaaS, and sending response to Api Gateway: "
                                            + responseFromService);
                            break;
                    }

                    writerToApiGateway.println(responseFromService);
                    writerToApiGateway.flush();
                } else {
                    writerToApiGateway.println(data);
                    writerToApiGateway.flush();
                }

            }

        } catch (IOException e) {
            switch (typeOfService) {
                case "Chat":
                    System.out.println(
                            "|| " + typeOfService + " -> || Exception: " + e.getMessage());
                    break;
                case "Login":
                    System.out.println(
                            "__ " + typeOfService + " -> __ Exception: " + e.getMessage());
                    break;
                case "File":
                    System.out.println(
                            "++ " + typeOfService + " -> ++ Exception: " + e.getMessage());
                    break;
                case "Post":
                    System.out.println(
                            "@@ " + typeOfService + " -> @@ Exception: " + e.getMessage());
                    break;
                case "Register":
                    System.out.println(
                            "!! " + typeOfService + " -> !! Exception: " + e.getMessage());
                    break;
            }
            // e.printStackTrace();
            // interrupt();
        }

        // System.out.println(typeOfService + " -> Handling the closing procedure by
        // closing connections.");
        // writerToApiGateway.close();
        // try {
        // readerFromApiGateway.close();
        // socketToApiGateway.close();
        // } catch (IOException e) {
        // System.out.println(typeOfService + " -> Exception: " + e.getMessage());
        // e.printStackTrace();
        // }

        // // Api Gateway -> source, Service -> dest
        // // TODO: 34100 -> Api Gateway port
        // String dataToAgent = "type:dest_service_session_close_info;message_id:" +
        // serviceInstance
        // +
        // ";sub_type:dest_service_to_agent;source_service_instance_network_address:localhost_"
        // + 34100
        // + ";source_plug_name:P;source_plug_port:" + 34100
        // + ";dest_service_name:" + typeOfService +
        // ";dest_service_instance_network_address:localhost_" + myPort
        // + ";dest_service_instance_id:" + serviceInstance +
        // ";dest_socket_name:S;dest_socket_port:" + myPort
        // + ";dest_socket_new_port:l";
        // serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
        // if (socketToService != null) {
        // writerToService.close();
        // try {
        // readerFromService.close();
        // socketToService.close();
        // } catch (IOException e) {
        // System.out.println(typeOfService + " -> Exception: " + e.getMessage());
        // e.printStackTrace();
        // }

        // // Service -> source, BaaS -> dest
        // dataToAgent = "type:source_service_session_close_info;message_id"
        // + serviceInstance
        // + ";sub_type:source_service_to_agent;source_service_name:" + typeOfService
        // + ";source_service_instance_network_address:localhost_"
        // + myPort
        // + ";source_service_instance_id:" + serviceInstance +
        // ";source_plug_name:P;source_plug_port:"
        // + myPort
        // + ";dest_service_name:BaaS;dest_service_instance_network_address:localhost_"
        // + myPort
        // + ";dest_socket_name:S;dest_socket_port:" + baasPort
        // + ";dest_socket_new_port:l";

        // serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
        // }

    }
}
