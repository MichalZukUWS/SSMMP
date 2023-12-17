import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

public class ServiceToAgentConnectionThread extends Thread {

    private Socket socketToAgent;
    private PrintWriter writerToAgent;
    private BufferedReader readerFromAgent;
    private LinkedList<String> requests;
    private LinkedList<String> responses;
    private String args;
    private ArrayList<ServiceThread> threads;
    private String typeOfService;
    private Thread processQueue;
    private Thread processResponses;
    // private boolean exit;

    public ServiceToAgentConnectionThread(
            LinkedList<String> requests,
            LinkedList<String> responses, String args, ArrayList<ServiceThread> threads, String typeOfService)
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
        this.typeOfService = typeOfService;
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
        // 3 -> service_name:Service1;
        // 4 -> service_instance:1;
        // 5 -> socket_configuration:localhost_34022;
        // 6 -> plug_configuration:configuration of plugs
        String initialDataToSend = "type:execution_response;" + args.split(";")[1]
                + ";status:200";
        switch (typeOfService) {
            case "Chat":
                System.out.println(
                        "|| " + typeOfService + " -> || Sending registration data: " + initialDataToSend);
                break;
            case "Login":
                System.out.println(
                        "__ " + typeOfService + " -> __ Sending registration data: " + initialDataToSend);
                break;
            case "File":
                System.out.println(
                        "++ " + typeOfService + " -> ++ Sending registration data: " + initialDataToSend);
                break;
            case "Post":
                System.out.println(
                        "@@ " + typeOfService + " -> @@ Sending registration data: " + initialDataToSend);
                break;
            case "Register":
                System.out.println(
                        "!! " + typeOfService + " -> !! Sending registration data: " + initialDataToSend);
                break;
        }
        writerToAgent.println(initialDataToSend);
        writerToAgent.flush();

        processQueue = new Thread(() -> {
            while (!isInterrupted()) {
                synchronized (requests) {
                    if (requests.size() != 0) {
                        String data = requests.poll();
                        requests.notify();
                        switch (typeOfService) {
                            case "Chat":
                                System.out.println(
                                        "|| " + typeOfService + " -> || To the agent sent: " + data);
                                break;
                            case "Login":
                                System.out.println(
                                        "__ " + typeOfService + " -> __ To the agent sent: " + data);
                                break;
                            case "File":
                                System.out.println(
                                        "++ " + typeOfService + " -> ++ To the agent sent: " + data);
                                break;
                            case "Post":
                                System.out.println(
                                        "@@ " + typeOfService + " -> @@ To the agent sent: " + data);
                                break;
                            case "Register":
                                System.out.println(
                                        "!! " + typeOfService + " -> !! To the agent sent: " + data);
                                break;
                        }
                        writerToAgent.println(data);
                        writerToAgent.flush();
                    }
                }
            }
            writerToAgent.close();
            switch (typeOfService) {
                case "Chat":
                    System.out.println(
                            "|| " + typeOfService
                                    + " -> || Closed thread which is responsible for sending data to Agent.");
                    break;
                case "Login":
                    System.out.println(
                            "__ " + typeOfService
                                    + " -> __ Closed thread which is responsible for sending data to Agent.");
                    break;
                case "File":
                    System.out.println(
                            "++ " + typeOfService
                                    + " -> ++ Closed thread which is responsible for sending data to Agent.");
                    break;
                case "Post":
                    System.out.println(
                            "@@ " + typeOfService
                                    + " -> @@ Closed thread which is responsible for sending data to Agent.");
                    break;
                case "Register":
                    System.out.println(
                            "!! " + typeOfService
                                    + " -> !! Closed thread which is responsible for sending data to Agent.");
                    break;
            }
        });

        processResponses = new Thread(() -> {
            while (!isInterrupted()) {
                try {
                    String responseFromAgent = readerFromAgent.readLine();
                    switch (typeOfService) {
                        case "Chat":
                            System.out.println(
                                    "|| " + typeOfService + " -> || From the agent received: " + responseFromAgent);
                            break;
                        case "Login":
                            System.out.println(
                                    "__ " + typeOfService + " -> __ From the agent received: " + responseFromAgent);
                            break;
                        case "File":
                            System.out.println(
                                    "++ " + typeOfService + " -> ++ From the agent received: " + responseFromAgent);
                            break;
                        case "Post":
                            System.out.println(
                                    "@@ " + typeOfService + " -> @@ From the agent received: " + responseFromAgent);
                            break;
                        case "Register":
                            System.out.println(
                                    "!! " + typeOfService + " -> !! From the agent received: " + responseFromAgent);
                            break;
                    }
                    if (!responseFromAgent.split(";")[0].split(":")[1].equalsIgnoreCase("health_control_request")) {
                        if (responseFromAgent.split(";")[0].split(":")[1]
                                .equalsIgnoreCase("graceful_shutdown_request")) {
                            switch (typeOfService) {
                                case "Chat":
                                    System.out.println(
                                            "|| " + typeOfService + " -> || Received a request to close.");
                                    System.out.println(
                                            "|| " + typeOfService + " -> || Closing " + typeOfService + " threads.");
                                    break;
                                case "Login":
                                    System.out.println(
                                            "__ " + typeOfService + " -> __ Received a request to close.");
                                    System.out.println(
                                            "__ " + typeOfService + " -> __ Closing " + typeOfService + " threads.");
                                    break;
                                case "File":
                                    System.out.println(
                                            "++ " + typeOfService + " -> ++ Received a request to close.");
                                    System.out.println(
                                            "++ " + typeOfService + " -> ++ Closing " + typeOfService + " threads.");
                                    break;
                                case "Post":
                                    System.out.println(
                                            "@@ " + typeOfService + " -> @@ Received a request to close.");
                                    System.out.println(
                                            "@@ " + typeOfService + " -> @@ Closing " + typeOfService + " threads.");
                                    break;
                                case "Register":
                                    System.out.println(
                                            "!! " + typeOfService + " -> !! Received a request to close.");
                                    System.out.println(
                                            "!! " + typeOfService + " -> !! Closing " + typeOfService + " threads.");
                                    break;
                            }
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
                            switch (typeOfService) {
                                case "Chat":
                                    System.out.println(
                                            "|| " + typeOfService + " -> || Closing application.");
                                    break;
                                case "Login":
                                    System.out.println(
                                            "__ " + typeOfService + " -> __ Closing application.");
                                    break;
                                case "File":
                                    System.out.println(
                                            "++ " + typeOfService + " -> ++ Closing application.");
                                    break;
                                case "Post":
                                    System.out.println(
                                            "@@ " + typeOfService + " -> @@ Closing application.");
                                    break;
                                case "Register":
                                    System.out.println(
                                            "!! " + typeOfService + " -> !! Closing application.");
                                    break;
                            }
                            System.exit(0);
                            // System.out.println(typeOfService + " -> Closing application.");
                            // System.exit(0);
                        } else {
                            if (responseFromAgent.split(";")[0].split(":")[1]
                                    .equalsIgnoreCase("source_service_session_close_request")) {
                                // TODO: add to ServiceThread some informations and close only one connection
                                synchronized (threads) {
                                    threads.forEach(t -> t.closeConnectionWithBaaS(responseFromAgent));
                                    threads.notify();
                                }
                            } else {
                                synchronized (responses) {
                                    responses.add(responseFromAgent);
                                    responses.notify();
                                }
                            }
                        }

                    } else {
                        // TODO: actually check the status of the service
                        String healthResponse = "type:health_control_response;message_id:"
                                + responseFromAgent.split(";")[1].split(":")[1]
                                + ";sub_type:service_instance_to_agent;service_name:"
                                + typeOfService + ";service_instance_id:"
                                + responseFromAgent.split(";")[4].split(":")[1] + ";status:200";
                        addRequestToAgent(healthResponse);
                    }
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
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            switch (typeOfService) {
                case "Chat":
                    System.out.println(
                            "|| " + typeOfService
                                    + " -> || Closed thread which is resposible for reading data from Agent.");
                    break;
                case "Login":
                    System.out.println(
                            "__ " + typeOfService
                                    + " -> __ Closed thread which is resposible for reading data from Agent.");
                    break;
                case "File":
                    System.out.println(
                            "++ " + typeOfService
                                    + " -> ++ Closed thread which is resposible for reading data from Agent.");
                    break;
                case "Post":
                    System.out.println(
                            "@@ " + typeOfService
                                    + " -> @@ Closed thread which is resposible for reading data from Agent.");
                    break;
                case "Register":
                    System.out.println(
                            "!! " + typeOfService
                                    + " -> !! Closed thread which is resposible for reading data from Agent.");
                    break;
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
        // System.out.println(typeOfService + " -> Closing application.");
        // System.exit(0);
        // } catch (InterruptedException e) {
        // System.out.println(typeOfService + " -> Exception: " + e.getMessage());
        // e.printStackTrace();
        // }
        // } else {
        // try {
        // Thread.sleep(10);
        // } catch (InterruptedException e) {
        // System.out.println(typeOfService + " -> Exception: " + e.getMessage());
        // e.printStackTrace();
        // }
        // }
        // }

    }

}
