import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class ApiGatewayToAgentConnectionThread extends Thread {

    private Socket socketToAgent;
    private PrintWriter writerToAgent;
    private BufferedReader readerFromAgent;
    private LinkedList<String> requests;
    private LinkedList<String> responses;

    public ApiGatewayToAgentConnectionThread(LinkedList<String> requests,
            LinkedList<String> responses) throws UnknownHostException, IOException {
        this.requests = requests;
        this.responses = responses;
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

        String initialData = "type:connection_request;message_id:20;socket_configuration:localhost_" + 34100
                + ";status:200";

        System.out.println("Api Gateway -> Data sent during registration: " + initialData);

        writerToAgent.println(initialData);
        writerToAgent.flush();

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

        Thread processResponses = new Thread(() -> {
            while (!isInterrupted()) {
                try {
                    if (readerFromAgent.ready()) {
                        String responseFromAgent = readerFromAgent.readLine();
                        System.out
                                .println("Api Gateway -> Received response from agent: " + responseFromAgent);
                        synchronized (responses) {
                            responses.add(responseFromAgent);
                            responses.notify();
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
