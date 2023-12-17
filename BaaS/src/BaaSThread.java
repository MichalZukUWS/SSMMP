import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

public class BaaSThread extends Thread {

    public Socket socketFromService;
    public PrintWriter writerToService;
    public BufferedReader readerFromService;
    private BaaSToAgentConnectionThread serviceToAgentConnectionThread;
    private LinkedList<String> responses;
    private int myPort;
    private int serviceInstance;
    private ArrayList<BaaSThread> threads;

    public BaaSThread(Socket socket, BaaSToAgentConnectionThread serviceToAgentConnectionThread,
            LinkedList<String> responses, int myPort, int serviceInstance,
            ArrayList<BaaSThread> threads) throws IOException {
        this.socketFromService = socket;
        this.serviceToAgentConnectionThread = serviceToAgentConnectionThread;
        this.responses = responses;
        this.serviceInstance = serviceInstance;
        writerToService = new PrintWriter(this.socketFromService.getOutputStream());
        readerFromService = new BufferedReader(new InputStreamReader(this.socketFromService.getInputStream()));
        this.myPort = myPort;
        this.threads = threads;
        start();
    }

    private String[] deconvertFromProtocole(String data) {
        return data.split(";");
    }

    @Override
    public void interrupt() {
        System.out.println("** BaaS -> ** Handling the closing procedure by closing connections.");
        writerToService.close();
        try {
            readerFromService.close();
            socketFromService.close();
        } catch (IOException e) {
            System.out
                    .println("** BaaS -> ** Exception with closing reader and socket from Service: " + e.getMessage());
            e.printStackTrace();
        }
        // Service -> source, ** BaaS -> ** dest
        // TODO: 123 -> Service port
        String dataToAgent = "type:dest_service_session_close_info;message_id:" + serviceInstance
                + ";sub_type:dest_service_to_agent;source_service_instance_network_address:localhost_" + 123
                + ";source_plug_name:P;source_plug_port:" + 123
                + ";dest_service_name:BaaS;dest_service_instance_network_address:localhost_" + myPort
                + ";dest_service_instance_id:" + serviceInstance + ";dest_socket_name:S;dest_socket_port:" + myPort
                + ";dest_socket_new_port:l";
        serviceToAgentConnectionThread.addRequestToAgent(dataToAgent);
    }

    @Override
    public void run() {
        try {
            DatabaseConnection connection = DatabaseConnection.getInstance();
            String sql = null;
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            int rowsAffected = -1;
            synchronized (threads) {
                threads.add(this);
                threads.notify();
            }
            while (!isInterrupted()) {

                // receiving and modifying data
                String data = readerFromService.readLine();
                System.out.println("** BaaS -> ** Received data from the Service: " + data);
                String[] decodedData = deconvertFromProtocole(data);

                serviceToAgentConnectionThread.addRequestToAgent(
                        "type:process_data;message_id:" + decodedData[1].split(":")[1]
                                + ";sub_type:Service_to_agent;source_name:BaaS;service_instance_id:"
                                + serviceInstance + "");

                switch (decodedData[0].split(":")[1]) {
                    case "register_request":
                        sql = "INSERT INTO users (login, password) VALUES (?, ?)";

                        statement = connection.getConnection().prepareStatement(sql);

                        statement.setString(1, decodedData[2].split(":")[1]);
                        statement.setString(2, decodedData[3].split(":")[1]);

                        rowsAffected = statement.executeUpdate();

                        if (rowsAffected == 1) {
                            data = "type:register_response;message_id:" + decodedData[1].split(":")[1]
                                    + ";status:200;data:Register successfull.";
                        } else {
                            data = "type:register_response;message_id:" + decodedData[1].split(":")[1]
                                    + ";status:300;data:Register unsuccessfull try later.";
                        }
                        // connection.getConnection().close();
                        break;

                    case "login_request":
                        sql = "SELECT ID FROM users WHERE login = ? AND password = ?";

                        statement = connection.getConnection().prepareStatement(sql);

                        statement.setString(1, decodedData[2].split(":")[1]);
                        statement.setString(2, decodedData[3].split(":")[1]);

                        resultSet = statement.executeQuery();

                        if (resultSet.next()) {
                            data = "type:login_response;message_id:" + decodedData[1].split(":")[1]
                                    + ";status:200;data:Login successfull.";
                        } else {
                            data = "type:login_response;message_id:" + decodedData[1].split(":")[1]
                                    + ";status:300;data:Login unsuccessfull wrong login or password.";
                        }

                        // connection.getConnection().close();
                        break;

                    case "display_posts_request":
                        sql = "SELECT users.login, chats.post FROM users INNER JOIN chats on users.ID = chats.userID ORDER BY chats.ID DESC LIMIT 10";

                        statement = connection.getConnection().prepareStatement(sql);

                        resultSet = statement.executeQuery();

                        data = "type:display_posts_response;message_id:" + decodedData[1].split(":")[1]
                                + ";status:200;";

                        while (resultSet.next()) {
                            String login = resultSet.getString("login");
                            String post = resultSet.getString("post");
                            data += "data:" + login + " -> " + post + ";";
                        }

                        // connection.getConnection().close();

                        break;
                    case "chat_request":
                        sql = "SELECT ID FROM users WHERE login = ?";

                        statement = connection.getConnection().prepareStatement(sql);

                        statement.setString(1, decodedData[2].split(":")[1]);

                        resultSet = statement.executeQuery();

                        if (resultSet.next()) {
                            int userID = resultSet.getInt("ID");

                            sql = "INSERT INTO chats (userID, post) VALUES (?, ?)";

                            statement = connection.getConnection().prepareStatement(sql);

                            statement.setInt(1, userID);
                            statement.setString(2, decodedData[3].split(":")[1]);

                            rowsAffected = statement.executeUpdate();

                            if (rowsAffected == 1) {
                                data = "type:chat_response;message_id:" + decodedData[1].split(":")[1]
                                        + ";status:200;data:Successfully added chat message.";
                            } else {
                                data = "type:chat_response;message_id:" + decodedData[1].split(":")[1]
                                        + ";status:300;data:Unsuccessfully added chat message try later.";
                            }
                        } else {
                            data = "type:chat_response;message_id:" + decodedData[1].split(":")[1]
                                    + ";status:300;data:User with login: " + decodedData[2].split(":")[1]
                                    + " doesn't exist in database.";
                        }
                        // connection.getConnection().close();
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
            System.out.println("** BaaS -> ** Exception on reading and writing with Service: " + e.getMessage());
            // e.printStackTrace();
            // interrupt();
        } catch (SQLException e) {
            System.out.println("** BaaS -> ** Exception on SQL query: " + e.getMessage());
            // e.printStackTrace();
            // interrupt();
        } catch (ClassNotFoundException e) {
            System.out.println("** BaaS -> ** Exception on loading external class: " + e.getMessage());
            // e.printStackTrace();
            // interrupt();
        } catch (NullPointerException e) {
            System.out.println("** BaaS -> ** Exception on processing data: " + e.getMessage());
            // e.printStackTrace();
            // interrupt();
        }
    }
}
