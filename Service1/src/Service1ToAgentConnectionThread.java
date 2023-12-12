import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

public class Service1ToAgentConnectionThread extends Thread {

    private Socket socketToAgent;
    private PrintWriter writerToAgent;
    private BufferedReader readerFromAgent;
    private LinkedList<String> requests;
    private LinkedList<String> responses;
    private String args;
    private ArrayList<Service1Thread> threads;

    public Service1ToAgentConnectionThread(
            LinkedList<String> requests,
            LinkedList<String> responses, String args, ArrayList<Service1Thread> threads)
            throws UnknownHostException, IOException {
        this.requests = requests;
        this.responses = responses;
        this.args = args;
        // 2 -> agent_newtork_address:localhost_34020;
        socketToAgent = new Socket(args.split(";")[2].split(":")[1].split("_")[0],
                Integer.parseInt(args.split(";")[2].split(":")[1].split("_")[1]));
        writerToAgent = new PrintWriter(socketToAgent.getOutputStream());
        readerFromAgent = new BufferedReader(new InputStreamReader(socketToAgent.getInputStream()));
        this.threads = threads;
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

        // 0 -> type:execution_request;
        // 1 -> message_id:1000;
        // 2 -> agent_newtork_address:localhost_34020;
        // 3 -> service_name:Service1;
        // 4 -> service_instance:1;
        // 5 -> socket_configuration:localhost_34022;
        // 6 -> plug_configuration:configuration of plugs
        String initialDataToSend = "type:execution_response;" + args.split(";")[1]
                + ";socket_configuration:localhost_" + args.split(";")[5].split(":")[1].split("_")[1] + ";status:200";
        System.out.println("Service1 -> Sending registration data: " + initialDataToSend);
        writerToAgent.println(initialDataToSend);
        writerToAgent.flush();

        Thread processQueue = new Thread(() -> {
            while (!isInterrupted()) {
                synchronized (requests) {
                    if (requests.size() != 0) {
                        String data = requests.poll();
                        requests.notify();
                        System.out.println("Service1 -> To the agent sent: " + data);
                        writerToAgent.println(data);
                        writerToAgent.flush();
                    }
                }
            }
            System.out.println("Service1 -> Closed thread which is responsible for sending data to Agent.");
        });

        Thread processResponses = new Thread(() -> {
            while (!isInterrupted()) {
                try {
                    String responseFromAgent = readerFromAgent.readLine();
                    System.out.println("Service1 -> From the agent received: " + responseFromAgent);
                    if (!responseFromAgent.split(";")[0].split(":")[1].equalsIgnoreCase("health_control_request")) {
                        if (responseFromAgent.split(";")[0].split(":")[1]
                                .equalsIgnoreCase("graceful_shutdown_request")) {
                            System.out.println("Service1 -> Received a request to close.");
                            System.out.println("Service1 -> Closing service1 threads.");
                            threads.stream().forEach(t -> t.interrupt());
                            synchronized (requests) {
                                // TODO: send diffrent status code in case of failure
                                requests.add("type:graceful_shutdown_response;message_id:"
                                        + responseFromAgent.split(";")[1].split(":")[1]
                                        + ";sub_type: Service_instance_to_agent;status:200");
                                requests.notify();
                            }
                            Thread.sleep(2000);
                            System.out.println("Service1 -> Closing application.");
                            System.exit(0);
                        } else {
                            synchronized (responses) {
                                responses.add(responseFromAgent);
                                responses.notify();
                            }
                        }

                    } else {
                        // TODO: replace serviceInstance
                        // TODO: actually check the status of the service
                        String healthResponse = "type:health_control_response;message_id:10;sub_type:service_instance_to_agent;service_name:Service1;service_instance_id:i;status:200";
                        addRequestToAgent(healthResponse);
                    }
                } catch (IOException e) {
                    System.out.println("Service1 Exception: " + e.getMessage());
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    System.out.println("Service1 Exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            System.out.println("Service1 -> Closed thread which is resposible for reading data from Agent.");
        });

        processQueue.start();
        processResponses.start();
    }

}
