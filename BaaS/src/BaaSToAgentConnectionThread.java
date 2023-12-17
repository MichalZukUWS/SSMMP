import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

public class BaaSToAgentConnectionThread extends Thread {

    private Socket socketToAgent;
    private PrintWriter writerToAgent;
    private BufferedReader readerFromAgent;
    private LinkedList<String> requests;
    private LinkedList<String> responses;
    private ArrayList<BaaSThread> threads;
    private String args;
    private int serviceInstance;
    private Thread processQueue;
    private Thread processResponses;
    // private boolean exit;

    public BaaSToAgentConnectionThread(
            LinkedList<String> requests,
            LinkedList<String> responses, String args,
            ArrayList<BaaSThread> threads, int serviceInstance) throws UnknownHostException, IOException {
        this.requests = requests;
        this.responses = responses;
        this.args = args;
        this.serviceInstance = serviceInstance;
        // 2 -> agent_newtork_address:localhost_34020;
        socketToAgent = new Socket(args.split(";")[2].split(":")[1].split("_")[0],
                Integer.parseInt(args.split(";")[2].split(":")[1].split("_")[1]));
        writerToAgent = new PrintWriter(socketToAgent.getOutputStream());
        readerFromAgent = new BufferedReader(new InputStreamReader(socketToAgent.getInputStream()));
        this.threads = threads;
        // exit = false;
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
        // 3 -> service_name:BaaS;
        // 4 -> service_instance:1;
        // 5 -> socket_configuration:localhost_34022;
        // 6 -> plug_configuration:configuration of plugss
        String initialDataToSend = "type:execution_response;" + args.split(";")[1]
                + ";status:200";
        System.out.println("** BaaS -> ** Sending registration data: " + initialDataToSend);
        writerToAgent.println(initialDataToSend);
        writerToAgent.flush();

        processQueue = new Thread(() -> {
            while (!isInterrupted()) {
                synchronized (requests) {
                    if (requests.size() != 0) {
                        String data = requests.poll();
                        requests.notify();
                        System.out.println("** BaaS -> ** To the agent I send: " + data);
                        writerToAgent.println(data);
                        writerToAgent.flush();
                    }
                }
            }
        });

        processResponses = new Thread(() -> {
            while (!isInterrupted()) {
                try {
                    String responseFromAgent = readerFromAgent.readLine();
                    System.out.println("** BaaS -> ** From the agent I received: " +
                            responseFromAgent);
                    if (!responseFromAgent.split(";")[0].split(":")[1].equalsIgnoreCase("health_control_request")) {

                        if (responseFromAgent.split(";")[0].split(":")[1]
                                .equalsIgnoreCase("graceful_shutdown_request")) {
                            System.out.println("** BaaS -> ** Received a request to close.");
                            System.out.println("** BaaS -> ** Closing threads.");
                            synchronized (threads) {
                                threads.forEach(t -> t.interrupt());
                                threads.notify();
                            }
                            Thread.sleep(2500);
                            synchronized (requests) {
                                // TODO: send diffrent status code in case of failure
                                requests.add("type:graceful_shutdown_response;message_id:"
                                        + responseFromAgent.split(";")[1].split(":")[1]
                                        + ";sub_type:Service_instance_to_agent;status:200");
                                requests.notify();
                            }
                            Thread.sleep(5000);
                            // processQueue.interrupt();
                            // processResponses.interrupt();
                            System.out.println("** BaaS -> ** Closing application.");
                            System.exit(0);
                        } else {
                            synchronized (responses) {
                                responses.add(responseFromAgent);
                                responses.notify();
                            }
                        }
                    } else {
                        // TODO: actually check the status of the service
                        String healthResponse = "type:health_control_response;message_id:" + serviceInstance
                                + ";sub_type:service_instance_to_agent;service_name:BaaS;service_instance_id:"
                                + serviceInstance + ";status:200";
                        addRequestToAgent(healthResponse);
                    }
                } catch (IOException e) {
                    System.out.println("** BaaS -> ** Exception: " + e.getMessage());
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        processQueue.start();
        processResponses.start();
        // while (!isInterrupted()) {
        // if (exit) {
        // try {
        // Thread.sleep(15000);
        // processResponses.interrupt();
        // processQueue.interrupt();
        // System.out.println("** BaaS -> ** Closing application.");
        // System.exit(0);
        // } catch (InterruptedException e) {
        // System.out.println("** BaaS -> ** Exception: " + e.getMessage());
        // e.printStackTrace();
        // }
        // } else {
        // try {
        // Thread.sleep(10);
        // } catch (InterruptedException e) {
        // System.out.println("** BaaS -> ** Exception: " + e.getMessage());
        // e.printStackTrace();
        // }
        // }
        // }

    }

}
