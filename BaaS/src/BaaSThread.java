import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;

public class BaaSThread extends Thread {

    public Socket socketFromService;
    public PrintWriter writerToService;
    public BufferedReader readerFromService;
    private BaaSToAgentConnectionThread serviceToAgentConnectionThread;
    private LinkedList<String> responses;
    private int myPort;
    private int serviceInstance;

    public BaaSThread(Socket socket, BaaSToAgentConnectionThread serviceToAgentConnectionThread,
            LinkedList<String> responses, int myPort, int serviceInstance) throws IOException {
        this.socketFromService = socket;
        this.serviceToAgentConnectionThread = serviceToAgentConnectionThread;
        this.responses = responses;
        this.serviceInstance = serviceInstance;
        writerToService = new PrintWriter(this.socketFromService.getOutputStream());
        readerFromService = new BufferedReader(new InputStreamReader(this.socketFromService.getInputStream()));
        this.myPort = myPort;
        start();
    }

    private String[] deconvertFromProtocole(String data) {
        return data.split(";");
    }

    @Override
    public void run() {
        try {
            while (true) {
                // receiving and modifying data
                String data = readerFromService.readLine();
                if (data == null)
                    break;
                System.out.println("BaaS -> Received data from the Service: " + data);
                String[] decodedData = deconvertFromProtocole(data);
                String words = decodedData[3].split(":")[1];
                data = decodedData[0] + ";" + decodedData[1] + ";" + "data: " + words + " ||| " + words;
                System.out.println("BaaS -> modified: " + data);

                System.out.println("BaaS -> Sending data to the service: " + data);

                writerToService.println(data);
                writerToService.flush();

            }

        } catch (IOException e) {
            System.out.println("BaaS Exception: " + e.getMessage());
            e.printStackTrace();
        }

        // Service1/2 -> source, BaaS -> dest
        // TODO what about message_id?
        // TODO 123 -> Service1/2 port
        String dataToAgent = "type:dest_service_session_close_info;message_id:" + 10
                + ";sub_type:dest_service_to_agent;source_service_instance_network_address:localhost_" + 123
                + ";source_plug_name:P;source_plug_port:" + 123
                + ";dest_service_name:BaaS;dest_service_instance_network_address:localhost_" + myPort
                + ";dest_service_instance_id:" + serviceInstance + ";dest_socket_name:S;dest_socket_port:" + myPort
                + ";dest_socket_new_port:l";
        serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
        writerToService.close();
        try {
            readerFromService.close();
            socketFromService.close();
        } catch (IOException e) {
            System.out.println("BaaS Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
