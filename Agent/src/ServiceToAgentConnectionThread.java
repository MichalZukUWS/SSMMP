import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;

public class ServiceToAgentConnectionThread extends Thread {

    private Socket socketFromService;
    private PrintWriter writerToService;
    private BufferedReader readerFromService;
    private LinkedList<String> requests;
    private HashMap<Integer, ServiceToAgentConnectionThread> servicePorts;
    private LinkedList<ServiceToAgentMessageWithPort> requestsToAgent;
    private Thread handleListDataThread;
    private Thread checkBufferThread;

    public ServiceToAgentConnectionThread(Socket socket,
            HashMap<Integer, ServiceToAgentConnectionThread> servicePorts,
            LinkedList<ServiceToAgentMessageWithPort> requestsToAgent) throws IOException {
        this.servicePorts = servicePorts;
        socketFromService = socket;
        writerToService = new PrintWriter(socketFromService.getOutputStream());
        readerFromService = new BufferedReader(new InputStreamReader(socketFromService.getInputStream()));
        requests = new LinkedList<>();
        this.requestsToAgent = requestsToAgent;
        start();
    }

    public void addRequestToService(String data) {
        synchronized (requests) {
            requests.add(data);
            requests.notify();
        }
    }

    @Override
    public void interrupt() {
        checkBufferThread.interrupt();
        handleListDataThread.interrupt();
        writerToService.close();
        try {
            readerFromService.close();
            socketFromService.close();
        } catch (IOException e) {
            System.out.println("Agent Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        // thread to check the buffer and handle the data
        checkBufferThread = new Thread(() -> {
            // check if new data is available in the buffer
            while (!isInterrupted()) {
                try {
                    if (readerFromService.ready()) {
                        // get data from the buffer
                        String data = readerFromService.readLine();
                        System.out.println(
                                "Agent -> I Received the data from the service: " + data);
                        if ((data.split(";")[0].split(":")[1].equalsIgnoreCase("execution_response"))) {
                            synchronized (servicePorts) {
                                System.out.println("Agent -> Putted in servicePorts map port: "
                                        + data.split(";")[2].split(":")[1].split("_")[1] + " with this thread");
                                servicePorts.put(
                                        Integer.parseInt(data.split(";")[2].split(":")[1].split("_")[1]), this);
                                servicePorts.notify();
                            }
                        }
                        synchronized (requestsToAgent) {
                            // System.out.println("Agent -> Adding to the queue:" + data);
                            requestsToAgent.add(new ServiceToAgentMessageWithPort(data,
                                    Integer.parseInt(data.split(";")[1].split(":")[1]), this));
                            requestsToAgent.notify();
                        }

                    }
                } catch (IOException e) {
                    System.out.println("Agent Exception: " + e.getMessage());
                    e.printStackTrace();
                    // this.interrupt();
                    checkBufferThread.interrupt();
                } catch (NumberFormatException e) {
                    System.out.println("Agent Exception: " + e.getMessage());
                    e.printStackTrace();
                    // this.interrupt();
                    checkBufferThread.interrupt();
                }
            }
            System.out.println("Agent -> Closed thread which is responsible for reading data from Service");
        });

        // create a thread to handle LinkedList data
        handleListDataThread = new Thread(() -> {
            while (!isInterrupted()) {
                try {
                    synchronized (requests) {
                        if (requests.size() != 0) {
                            String data = requests.poll();
                            requests.notify();
                            System.out.println("Agent -> Refer to the service data :" + data);
                            writerToService.println(data);
                            writerToService.flush();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Agent Exception: " + e.getMessage());
                    e.printStackTrace();
                    // this.interrupt();
                    handleListDataThread.interrupt();
                }

            }
            System.out.println("Agent -> Closed thread which is responsible for sending data to Service");
        });

        checkBufferThread.start();
        handleListDataThread.start();

    }

}
