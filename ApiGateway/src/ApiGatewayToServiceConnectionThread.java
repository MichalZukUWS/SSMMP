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

    public void send(Socket socketFromClient, String data) throws IOException {
        this.socketFromClient = socketFromClient;
        writerToClient = new PrintWriter(this.socketFromClient.getOutputStream());
        synchronized (requests) {
            requests.add(data);
            requests.notify();
        }
    }

    @Override
    public void run() {
        try {
            String dataToAgent = "type:session_request;message_id:" + firstData.split(";")[1].split(":")[1]
                    + ";sub_type:service_to_agent;source_service_name:Api Gateway;source_service_instance_id:100;source_plug_name:P;dest_service_name:";

            dataToAgent += firstData.split(";")[0].split(":")[1];

            nameOfService = firstData.split(";")[0].split(":")[1];

            dataToAgent += ";dest_socket_name:S";
            System.out.println("Api Gateway -> Data transferred to the agent:" + dataToAgent);
            apiGatewayToAgentConnectionThread.addRequestToAgent(dataToAgent);
            String response = null;

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
            socketToService = new Socket("localhost", servicePort);
            writerToService = new PrintWriter(socketToService.getOutputStream());
            readerFromService = new BufferedReader(new InputStreamReader(socketToService.getInputStream()));
            System.out.println("Api Gateway -> Established a connection to Service");

            dataToAgent = "type:session_ack;message_id:" + firstData.split(";")[1].split(":")[1]
                    + ";sub_type:service_to_agent;status:200;source_plug_port:34100;dest_socket_new_port:"
                    + servicePort;

            System.out
                    .println("Api Gateway -> Data to the Agent added to the queue of requests: "
                            + dataToAgent);

            apiGatewayToAgentConnectionThread.addRequestToAgent(dataToAgent);

            synchronized (threadsWithServices) {

                threadsWithServices.put(firstData.split(";")[0].split(":")[1], this);
            }
            writerToClient = new PrintWriter(socketFromClient.getOutputStream());
            synchronized (requests) {
                requests.add(firstData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (!isInterrupted()) {
            synchronized (requests) {
                if (!requests.isEmpty()) {
                    try {
                        String data = requests.poll();
                        requests.notify();
                        writerToService.println(data);
                        writerToService.flush();
                        String dataFromService = readerFromService.readLine();
                        writerToClient.println(dataFromService);
                        writerToClient.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

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
            e.printStackTrace();
        }

        // TODO what about message_id ?
        String dataToAgent = "type:source_service_session_close_info;message_id"
                + 10
                + ";sub_type:source_service_to_agent;source_service_name:Api Gateway;source_service_instance_network_address:localhost_"
                + apiGatewayPort
                + ";source_service_instance_id:100;source_plug_name:P;source_plug_port:"
                + apiGatewayPort
                + ";dest_service_name:" + nameOfService
                + ";dest_service_instance_network_address:localhost_" + servicePort
                + ";dest_socket_name:S;dest_socket_port:" + servicePort
                + ";dest_socket_new_port:l";

        apiGatewayToAgentConnectionThread.addRequestToAgent(dataToAgent);
    }
}
