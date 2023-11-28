import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class Service2ToAgentConnectionThread extends Thread {

    private Socket socketToAgent;
    private PrintWriter writerToAgent;
    private BufferedReader readerFromAgent;
    private LinkedList<String> requests;
    private LinkedList<String> responses;
    private String args;

    public Service2ToAgentConnectionThread(
            LinkedList<String> requests,
            LinkedList<String> responses, String args) throws UnknownHostException, IOException {
        this.requests = requests;
        this.responses = responses;
        this.args = args;
        // 2 -> agent_newtork_address:localhost_34020;
        socketToAgent = new Socket(args.split(";")[2].split(":")[1].split("_")[0],
                Integer.parseInt(args.split(";")[2].split(":")[1].split("_")[1]));
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

        // 0 -> type:execution_request;
        // 1 -> message_id:1000;
        // 2 -> agent_newtork_address:localhost_34020;
        // 3 -> service_name:Service2;
        // 4 -> service_instance:1;
        // 5 -> socket_configuration:localhost_34022;
        // 6 -> plug_configuration:configuration of plugs
        String initialDataToSend = "type:execution_response;" + args.split(";")[1] + ";status:200";
        System.out.println("Service2 -> Sending registration data: " + initialDataToSend);
        writerToAgent.println(initialDataToSend);
        writerToAgent.flush();

        Thread processQueue = new Thread(() -> {
            while (!isInterrupted()) {
                synchronized (requests) {
                    if (requests.size() != 0) {
                        String data = requests.poll();
                        requests.notify();
                        System.out.println("Service2 -> To the agent sent: " + data);
                        writerToAgent.println(data);
                        writerToAgent.flush();
                    }
                }
            }
        });

        Thread processResponses = new Thread(() -> {
            while (!isInterrupted()) {
                try {
                    String responseFromAgent = readerFromAgent.readLine();
                    System.out.println("Service2 -> From the agent received: " + responseFromAgent);
                    synchronized (responses) {
                        responses.add(responseFromAgent);
                        responses.notify();
                    }
                } catch (IOException e) {
                    System.out.println("Service2 Exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        processQueue.start();
        processResponses.start();
    }

}