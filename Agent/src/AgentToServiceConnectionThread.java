import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;

public class AgentToServiceConnectionThread extends Thread {

    private Socket socketFromService;
    private PrintWriter writerToService;
    private BufferedReader readerFromService;
    private LinkedList<String> requests;
    private Thread handleListDataThread;
    private Thread checkBufferThread;
    private AgentToManagerConnectionThread agentToManagerConnectionThread;
    private ArrayList<ServiceConnectionWithAgentEntry> serivcePorts;

    public AgentToServiceConnectionThread(Socket socket,
            HashMap<Integer, AgentToServiceConnectionThread> servicePorts,
            AgentToManagerConnectionThread agentToManagerConnectionThread,
            ArrayList<ServiceConnectionWithAgentEntry> serivcePorts) throws IOException {
        socketFromService = socket;
        writerToService = new PrintWriter(socketFromService.getOutputStream());
        readerFromService = new BufferedReader(new InputStreamReader(socketFromService.getInputStream()));
        requests = new LinkedList<>();
        this.serivcePorts = serivcePorts;
        this.agentToManagerConnectionThread = agentToManagerConnectionThread;
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
                                "-- Agent -> -- I Received the data from the service: " + data);
                        if ((data.split(";")[0].split(":")[1].equalsIgnoreCase("execution_response"))) {
                            synchronized (serivcePorts) {
                                Optional<ServiceConnectionWithAgentEntry> entry = serivcePorts.stream().filter(
                                        e -> e.getMessage_id() == Integer.parseInt(data.split(";")[1].split(":")[1]))
                                        .findFirst();
                                if (entry.isPresent()) {
                                    entry.get().setServiceToAgentConnectionThread(this);
                                    entry.get().setMessage_id(-1);

                                } else {
                                    System.out.println(
                                            "-- Agent -> -- Received a message about the launch of a service that is not supposed to launch");
                                }
                                serivcePorts.notify();
                            }
                        }
                        if ((data.split(";")[0].split(":")[1].equalsIgnoreCase("graceful_shutdown_response"))) {
                            // TODO: add check if the operation was successful
                            synchronized (serivcePorts) {
                                serivcePorts.removeIf(
                                        s -> s.getMessage_id() == Integer.parseInt(data.split(";")[1].split(":")[1]));

                                serivcePorts.notify();
                            }
                        }
                        agentToManagerConnectionThread.addDataFromService(data,
                                Integer.parseInt(data.split(";")[1].split(":")[1]), this);

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
            System.out.println("-- Agent -> -- Closed thread which is responsible for reading data from Service");
        });

        // create a thread to handle LinkedList data
        handleListDataThread = new Thread(() -> {
            while (!isInterrupted()) {
                try {
                    synchronized (requests) {
                        if (requests.size() != 0) {
                            String data = requests.poll();
                            requests.notify();
                            System.out.println("-- Agent -> -- Refer to the service data :" + data);
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
            System.out.println("-- Agent -> -- Closed thread which is responsible for sending data to Service");
        });

        checkBufferThread.start();
        handleListDataThread.start();

    }

}
