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
    private Socket socketToService;
    private PrintWriter writerToService;
    private BufferedReader readerFromService;
    private ApiGatewayToAgentConnectionThread serviceToAgentConnectionThread;
    private LinkedList<String> responses;
    private String serviceNameNowConnected;
    private int servicePortNowConnected;
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
        serviceNameNowConnected = null;
        this.threadsWithServices = threadsWithServices;
        start();
    }

    public void foo() {

    }

    private String[] deconvertFromProtocole(String data) {
        return data.split(";");
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                String data = readerFromClient.readLine();
                if (data == null)
                    break;

                String[] decodedData = deconvertFromProtocole(data);

                synchronized (threadsWithServices) {
                    if (threadsWithServices.keySet().contains(decodedData[0].split(":")[1])) {
                        threadsWithServices.get(decodedData[0].split(":")[1]).send(socketFromClient, data);
                        threadsWithServices.notify();
                    } else {
                        new ApiGatewayToServiceConnectionThread(socketFromClient, serviceToAgentConnectionThread,
                                threadsWithServices, responses, data, apiGatewayPort);
                    }

                }

                // // Api Gateway isn't connected to any of services
                // if (serviceNameNowConnected == null) {
                // System.out.println("Api Gateway -> Isn't connected to any of services");
                // // request for connection details
                // String dataToAgent = "type:session_request;message_id:" +
                // decodedData[1].split(":")[1]
                // + ";sub_type:service_to_agent;source_service_name:Api
                // Gateway;source_service_instance_id:100;source_plug_name:P;dest_service_name:";

                // dataToAgent += decodedData[0].split(":")[1];

                // dataToAgent += ";dest_socket_name:S";

                // System.out.println("Api Gateway -> Data transferred to the agent:" +
                // dataToAgent);

                // serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);

                // String response = null;

                // synchronized (responses) {
                // while (responses.size() == 0) {
                // try {
                // responses.wait();
                // } catch (InterruptedException e) {
                // System.out.println("Api Gateway Exception: " + e.getMessage());
                // e.printStackTrace();
                // }
                // }
                // response = responses.poll();
                // }

                // System.out.println("Api Gateway -> From the queue I received:" + response);

                // // 0 -> type:session_response;
                // // 1 -> message_id:1000;
                // // 2 -> sub_type:Manager_to_agent;
                // // 3 -> status:200;
                // // 4 -> dest_service_instance_network_address:localhost_36161;
                // // 5 -> dest_socket_port:36161
                // serviceNameNowConnected = decodedData[0].split(":")[1];
                // servicePortNowConnected =
                // Integer.parseInt(response.split(";")[5].split(":")[1]);
                // socketToService = new Socket("localhost", servicePortNowConnected);
                // writerToService = new PrintWriter(socketToService.getOutputStream());
                // readerFromService = new BufferedReader(new
                // InputStreamReader(socketToService.getInputStream()));
                // System.out.println("Api Gateway -> Established a connection to Service");

                // dataToAgent = "type:session_ack;message_id:" + decodedData[1].split(":")[1]
                // +
                // ";sub_type:service_to_agent;status:200;source_plug_port:34100;dest_socket_new_port:"
                // + servicePortNowConnected;

                // System.out
                // .println("Api Gateway -> Data to the Agent added to the queue of requests: "
                // + dataToAgent);

                // serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
                // } else {
                // // if isn't connected to appropriate service
                // if (!serviceNameNowConnected.equals(decodedData[0].split(":")[1])) {
                // System.out.println(
                // "Api Gateway -> Connected to: " + serviceNameNowConnected + " request for: "
                // + decodedData[0].split(":")[1]);
                // // close connection
                // writerToService.close();
                // readerFromService.close();
                // socketToService.close();
                // // send information about closing connection

                // // TODO source_service_instance_network_address:localhost_ ->
                // // source_service_instance_network_address:IP Address

                // String dataToAgent = "type:source_service_session_close_info;message_id:"
                // + decodedData[1].split(":")[1]
                // + ";sub_type:source_service_to_agent;source_service_name:Api
                // Gateway;source_service_instance_network_address:localhost_"
                // + apiGatewayPort
                // + ";source_service_instance_id:100;source_plug_name:P;source_plug_port:"
                // + apiGatewayPort
                // + ";dest_service_name:" + serviceNameNowConnected
                // + ";dest_service_instance_network_address:localhost_" +
                // servicePortNowConnected
                // + ";dest_socket_name:S;dest_socket_port:" + servicePortNowConnected
                // + ";dest_socket_new_port:l";

                // serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
                // // request for new connection
                // serviceNameNowConnected = decodedData[0].split(":")[1];
                // dataToAgent = "type:session_request;message_id:" +
                // decodedData[1].split(":")[1]
                // + ";sub_type:service_to_agent;source_service_name:Api
                // Gateway;source_service_instance_id:100;source_plug_name:P;dest_service_name:";

                // dataToAgent += decodedData[0].split(":")[1];

                // dataToAgent += ";dest_socket_name:S";

                // System.out.println("Api Gateway -> Data transferred to the agent:" +
                // dataToAgent);

                // serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);

                // String response = null;

                // synchronized (responses) {
                // while (responses.size() == 0) {
                // try {
                // responses.wait();
                // } catch (InterruptedException e) {
                // System.out.println("Api Gateway Exception: " + e.getMessage());
                // e.printStackTrace();
                // }
                // }
                // response = responses.poll();
                // }

                // System.out.println("Api Gateway -> From the queue I received:" + response);

                // // 0 -> type:session_response;
                // // 1 -> message_id:1000;
                // // 2 -> sub_type:Manager_to_agent;
                // // 3 -> status:200;
                // // 4 -> dest_service_instance_network_address:localhost_36161;
                // // 5 -> dest_socket_port:36161
                // servicePortNowConnected =
                // Integer.parseInt(response.split(";")[5].split(":")[1]);
                // socketToService = new Socket("localhost", servicePortNowConnected);
                // writerToService = new PrintWriter(socketToService.getOutputStream());
                // readerFromService = new BufferedReader(
                // new InputStreamReader(socketToService.getInputStream()));
                // System.out.println("Api Gateway -> Connected to the service");

                // dataToAgent = "type:session_ack;message_id:" + decodedData[1].split(":")[1]
                // + ";sub_type:service_to_agent;status:200;source_plug_port:" + apiGatewayPort
                // + ";dest_socket_new_port:"
                // + servicePortNowConnected;

                // System.out
                // .println("Api Gateway -> Data to the Agent added to the queue of requests: "
                // + dataToAgent);

                // serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
                // } else {
                // System.out.println("Api Gateway -> Isn't connected to requested Service");
                // }
                // }

                // System.out.println("Api gateway -> Send to Service: " + data);
                // writerToService.println(data);
                // writerToService.flush();
                // String response = readerFromService.readLine();
                // System.out.println("Api Gateway -> Received from Service: " + response);
                // writerToClient.println(response);
                // writerToClient.flush();

            }

        } catch (IOException e) {
            System.out.println("Api Gateway Exception: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            // client can connect to Api Gateway but it will send nothing
            // if (socketToService != null) {
            // // send message about closing connection with service

            // // TODO what about message_id ?
            // String dataToAgent = "type:source_service_session_close_info;message_id"
            // + 10
            // + ";sub_type:source_service_to_agent;source_service_name:Api
            // Gateway;source_service_instance_network_address:localhost_"
            // + apiGatewayPort
            // + ";source_service_instance_id:100;source_plug_name:P;source_plug_port:"
            // + apiGatewayPort
            // + ";dest_service_name:" + serviceNameNowConnected
            // + ";dest_service_instance_network_address:localhost_" +
            // servicePortNowConnected
            // + ";dest_socket_name:S;dest_socket_port:" + servicePortNowConnected
            // + ";dest_socket_new_port:l";

            // serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);

            // readerFromService.close();
            // writerToService.close();
            // socketToService.close();
            // }
            readerFromClient.close();
            writerToClient.close();
            socketFromClient.close();
        } catch (IOException e) {
            System.out.println("Api Gateway Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
