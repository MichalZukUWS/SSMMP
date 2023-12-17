import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;

public class ApiGatewayToAgentConnectionThread extends Thread {

    private Socket socketToAgent;
    private PrintWriter writerToAgent;
    private BufferedReader readerFromAgent;
    private LinkedList<String> requests;
    private LinkedList<String> responses;
    private int startPort;
    private HashMap<String, ApiGatewayToServiceConnectionThread> threadsWithServices;

    public ApiGatewayToAgentConnectionThread(LinkedList<String> requests,
            LinkedList<String> responses, int startPort,
            HashMap<String, ApiGatewayToServiceConnectionThread> threadsWithServices)
            throws UnknownHostException, IOException {
        this.requests = requests;
        this.responses = responses;
        this.startPort = startPort;
        this.threadsWithServices = threadsWithServices;
        socketToAgent = new Socket("localhost", 34011);
        writerToAgent = new PrintWriter(socketToAgent.getOutputStream());
        readerFromAgent = new BufferedReader(new InputStreamReader(socketToAgent.getInputStream()));
        start();
    }

    public void addRequestToAgent(String dataToManager) {
        synchronized (requests) {
            requests.add(dataToManager);
            requests.notify();
        }
    }

    @Override
    public void run() {

        // request about registration Api Gateway in Manager
        String initialData = "type:connection_request;message_id:1;socket_configuration:localhost_" + startPort
                + ";status:200";

        System.out.println("Api Gateway -> Data sent during registration: " + initialData);

        writerToAgent.println(initialData);
        writerToAgent.flush();

        // Thread to process request to Agent
        Thread processQueue = new Thread(() -> {
            while (!isInterrupted()) {
                synchronized (requests) {
                    if (requests.size() != 0) {
                        String data = requests.poll();
                        System.out.println("Api Gateway -> To the agent send: " + data);
                        writerToAgent.println(data);
                        writerToAgent.flush();
                    }
                }
            }

        });

        // Thread to process request or responses from Agent
        Thread processResponses = new Thread(() -> {
            while (!isInterrupted()) {
                try {
                    if (readerFromAgent.ready()) {
                        String responseFromAgent = readerFromAgent.readLine();
                        System.out
                                .println("Api Gateway -> Received response from agent: " + responseFromAgent);
                        if (responseFromAgent.split(";")[0].split(":")[1].equalsIgnoreCase("health_control_request")) {
                            // check health about Api Gateway
                            // TODO:: actually check the status of the service
                            String healthResponse = "type:health_control_response;message_id:"
                                    + responseFromAgent.split(";")[1].split(":")[1]
                                    + ";sub_type:service_instance_to_agent;service_name:Api Gateway;service_instance_id:10;status:200";
                            addRequestToAgent(healthResponse);
                        } else if (responseFromAgent.split(";")[0].split(":")[1]
                                .equalsIgnoreCase("source_service_session_close_request")) {
                            synchronized (threadsWithServices) {
                                threadsWithServices.get(responseFromAgent.split(";")[8].split(":")[1])
                                        .closeConnectionWithService(responseFromAgent);
                            }
                        } else {
                            synchronized (responses) {
                                responses.add(responseFromAgent);
                                responses.notify();
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Api Gateway Exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        });

        processQueue.start();
        processResponses.start();
    }

}
