import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;

public class Service2Thread extends Thread {

    public Socket socketToApiGateway;
    public PrintWriter writerToApiGateway;
    public BufferedReader readerFromApiGateway;
    private Service2ToAgentConnectionThread serviceToAgentConnectionThread;
    private LinkedList<String> responses;
    public Socket socketToService;
    public PrintWriter writerToService;
    public BufferedReader readerFromService;
    private int baasPort;

    private int myPort;
    private int serviceInstance;

    public Service2Thread(Socket socket, Service2ToAgentConnectionThread serviceToAgentConnectionThread,
            LinkedList<String> responses, int myPort, int serviceInstance) throws IOException {
        this.socketToApiGateway = socket;
        this.serviceToAgentConnectionThread = serviceToAgentConnectionThread;
        this.responses = responses;
        this.serviceInstance = serviceInstance;
        writerToApiGateway = new PrintWriter(this.socketToApiGateway.getOutputStream());
        readerFromApiGateway = new BufferedReader(new InputStreamReader(this.socketToApiGateway.getInputStream()));
        this.myPort = myPort;
        start();
    }

    private String[] deconvertFromProtocole(String data) {
        return data.split(";");
    }

    @Override
    public void run() {
        try {
            while (true) {
                String data = readerFromApiGateway.readLine();
                if (data == null)
                    break;
                System.out.println("Service2 -> Received data from the agent: " + data);
                String[] decodedData = deconvertFromProtocole(data);
                String words = decodedData[2].split(":")[1].toUpperCase();
                data = decodedData[0] + ";" + decodedData[1] + ";" + "Data:" + words;
                System.out.println("Service2 -> modified: " + data);
                String toSend = null;
                if (socketToService == null) {
                    // send request for the port

                    toSend = "type:session_request;" + decodedData[1]
                            + ";sub_type:service_to_agent;source_service_name:Service2;source_service_instance_id:"
                            + serviceInstance + ";source_plug_name:P;dest_service_name:BaaS;dest_socket_name:S";
                    System.out.println("Service2 -> Adds to the queue of requests to the Agent: " + toSend);

                    serviceToAgentConnectionThread.addRequestToAgent(toSend);

                    String responseFromAgent = null;

                    synchronized (responses) {
                        while (responses.size() == 0) {
                            try {
                                responses.wait();
                            } catch (InterruptedException e) {
                                System.out.println("Service2 Exception: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        responseFromAgent = responses.poll();
                    }

                    System.out.println("Service2 -> Received data from Agent from the queue: " + responseFromAgent);
                    baasPort = Integer.parseInt(responseFromAgent.split(";")[5].split(":")[1]);

                    socketToService = new Socket("localhost", baasPort);
                    writerToService = new PrintWriter(socketToService.getOutputStream());
                    readerFromService = new BufferedReader(new InputStreamReader(socketToService.getInputStream()));

                    System.out.println("Service2 -> Established connection with BaaS");

                    toSend = "type:session_ack;" + decodedData[1]
                            + ";sub_type:service_to_agent;status:200;source_plug_port:" + myPort
                            + ";dest_socket_new_port:"
                            + baasPort;

                    System.out.println("Service2 -> Sending data to the agent: " + toSend);

                    serviceToAgentConnectionThread.addRequestToAgent(toSend);

                }

                toSend = "type:Service2;" + decodedData[1] + ";status:200;data:" + words;

                System.out.println("Service2 -> Sending data to the service: " + toSend);

                writerToService.println(toSend);
                writerToService.flush();

                String responseFromService = readerFromService.readLine();

                System.out.println("Service2 -> Received data from BaaS, and sending response to Api Gateway: "
                        + responseFromService);

                writerToApiGateway.println(responseFromService);
                writerToApiGateway.flush();

            }

        } catch (IOException e) {
            System.out.println("Service2 Exception: " + e.getMessage());
            e.printStackTrace();
        }

        writerToApiGateway.close();
        try {
            readerFromApiGateway.close();
            socketToApiGateway.close();
        } catch (IOException e) {
            System.out.println("Service2 Exception: " + e.getMessage());
            e.printStackTrace();
        }

        // Api Gateway -> source, Service2 -> dest
        // TODO what about message_id?
        // TODO 123 -> Api Gateway port
        String dataToAgent = "type:dest_service_session_close_info;message_id:" + 10
                + ";sub_type:dest_service_to_agent;source_service_instance_network_address:localhost_" + 123
                + ";source_plug_name:P;source_plug_port:" + 123
                + ";dest_service_name:Service2;dest_service_instance_network_address:localhost_" + myPort
                + ";dest_service_instance_id:" + serviceInstance + ";dest_socket_name:S;dest_socket_port:" + myPort
                + ";dest_socket_new_port:l";
        serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
        writerToService.close();
        try {
            readerFromService.close();
            socketToService.close();
        } catch (IOException e) {
            System.out.println("Service2 Exception: " + e.getMessage());
            e.printStackTrace();
        }

        // Service2 -> source, BaaS -> dest
        // TODO what about message_id?
        dataToAgent = "type:source_service_session_close_info;message_id"
                + 10
                + ";sub_type:source_service_to_agent;source_service_name:Api Gateway;source_service_instance_network_address:localhost_"
                + myPort
                + ";source_service_instance_id:100;source_plug_name:P;source_plug_port:"
                + myPort
                + ";dest_service_name:BaaS;dest_service_instance_network_address:localhost_" + myPort
                + ";dest_socket_name:S;dest_socket_port:" + baasPort
                + ";dest_socket_new_port:l";

        serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
    }
}
