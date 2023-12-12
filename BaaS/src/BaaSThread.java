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
            while (!isInterrupted()) {

                // receiving and modifying data
                String data = readerFromService.readLine();
                System.out.println("BaaS -> Received data from the Service: " + data);
                String[] decodedData = deconvertFromProtocole(data);

                switch (decodedData[0].split(":")[1]) {
                    case "register_request":
                        data = "type:register_response;message_id:" + decodedData[1].split(":")[1]
                                + ";status:200;data:Register successfull.";
                        break;

                    case "login_request":
                        data = "type:login_response;message_id:" + decodedData[1].split(":")[1]
                                + ";status:200;data:Login successfull.";
                        break;

                    case "display_posts_request":
                        data = "type:display_posts_response;message_id:" + decodedData[1].split(":")[1]
                                + ";status:200;data:Test -> Test text;data:Test2 -> Test2;data:Test3 -> Test3";
                        break;
                    case "chat_request":
                        data = "type:chat_response;message_id:" + decodedData[1].split(":")[1]
                                + ";status:200;data:Successfully added chat post.";
                        break;

                    case "file_upload_request":
                        data = "type:file_upload_response;message_id:" + decodedData[1].split(":")[1]
                                + ";status:300;data:Needs implementation.";
                        break;

                    case "file_download_request":
                        data = "type:file_download_response;message_id:" + decodedData[1].split(":")[1]
                                + ";status:300;data:Needs implementation.";
                        break;
                    default:
                        break;
                }

                writerToService.println(data);
                writerToService.flush();

            }
        } catch (IOException e) {
            System.out.println("BaaS Exception on reading and writing with Service: " + e.getMessage());
            e.printStackTrace();
            interrupt();
        }

        // Service1/2 -> source, BaaS -> dest
        // TODO: what about message_id?
        // TODO: 123 -> Service1/2 port
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
            System.out.println("BaaS Exception with closing reader and socket from Service: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
