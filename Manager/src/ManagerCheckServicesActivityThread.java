import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ManagerCheckServicesActivityThread extends Thread {

    private BaaSConnections baaSConnections;
    private ChatConnections chatConnections;
    private FileConnections fileConnections;
    private LoginConnections loginConnections;
    private PostsConnections postsConnections;
    private RegisterConnections registerConnections;
    private LocalDateTime now;
    private AgentToManagerConnectionThread agentToManagerConnectionThread;

    public ManagerCheckServicesActivityThread(BaaSConnections baaSConnections, ChatConnections chatConnections,
            FileConnections fileConnections, LoginConnections loginConnections,
            PostsConnections postsConnections,
            RegisterConnections registerConnections,
            AgentToManagerConnectionThread agentToManagerConnectionThread) {
        this.baaSConnections = baaSConnections;
        this.chatConnections = chatConnections;
        this.fileConnections = fileConnections;
        this.postsConnections = postsConnections;
        this.registerConnections = registerConnections;
        this.baaSConnections = baaSConnections;
        this.loginConnections = loginConnections;
        this.agentToManagerConnectionThread = agentToManagerConnectionThread;
        start();
    }

    public void run() {
        while (!isInterrupted()) {
            now = LocalDateTime.now();

            // list of Services that are inactive for 30 seconds

            List<BaaSHistory> baasListToCloseConnection = baaSConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 30 && h.isConnected())
                    .toList();

            // TODO: add in Service History informations about Service to which it is connected
            // if BaaS service is innactive Services: Chat, File, Login, Post, Register needs to close connection with it
            if (baasListToCloseConnection.size() > 0) {
                System.out.println("/////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found connection with BaaS that is inactive for 30 seconds.");
                System.out.println("/////////////////////////////////////////////");
                baasListToCloseConnection
                        .forEach(b -> chatConnections
                                .getHistoryList()
                                .stream()
                                // Service needs to close connection with BaaS only if it was connected to it
                                .filter(h -> h.getHistory().stream()
                                        .anyMatch(a -> a.contains("BaaS")))
                                .forEach(r -> {
                                    agentToManagerConnectionThread
                                            .addRequestToAgent(
                                                    "type:source_service_session_close_request;message_id:"
                                                            + r.getServiceInstance()
                                                            + ";sub_type:Manager_to_agent;source_Service_name:Chat;source_Service_instance_network_address:localhost_"
                                                            + r.getPort()
                                                            + ";source_Service_instance_id:"
                                                            + r.getServiceInstance()
                                                            + ";source_plug_name:P;source_plug_port:"
                                                            + r.getPort()
                                                            + ";dest_Service_name:BaaS;dest_Service_instance_network_address:localhost_"
                                                            + b.getPort()
                                                            + ";dest_socket_name:S;dest_socket_port:"
                                                            + b.getPort()
                                                            + ";dest_socket_new_port:l");
                                    b.setConnected(false);
                                }));

                baasListToCloseConnection
                        .forEach(b -> fileConnections
                                .getHistoryList()
                                .stream()
                                // Service needs to close connection with BaaS only if it was connected to it
                                .filter(h -> h.getHistory().stream()
                                        .anyMatch(a -> a.contains("BaaS")))
                                .forEach(r -> {
                                    agentToManagerConnectionThread
                                            .addRequestToAgent(
                                                    "type:source_service_session_close_request;message_id:"
                                                            + r.getServiceInstance()
                                                            + ";sub_type:Manager_to_agent;source_Service_name:File;source_Service_instance_network_address:localhost_"
                                                            + r.getPort()
                                                            + ";source_Service_instance_id:"
                                                            + r.getServiceInstance()
                                                            + ";source_plug_name:P;source_plug_port:"
                                                            + r.getPort()
                                                            + ";dest_Service_name:BaaS;dest_Service_instance_network_address:localhost_"
                                                            + b.getPort()
                                                            + ";dest_socket_name:S;dest_socket_port:"
                                                            + b.getPort()
                                                            + ";dest_socket_new_port:l");
                                    b.setConnected(false);
                                }));
                baasListToCloseConnection
                        .forEach(b -> loginConnections
                                .getHistoryList()
                                .stream()
                                // Service needs to close connection with BaaS only if it was connected to it
                                .filter(h -> h.getHistory().stream()
                                        .anyMatch(a -> a.contains("BaaS")))
                                .forEach(r -> {
                                    agentToManagerConnectionThread
                                            .addRequestToAgent(
                                                    "type:source_service_session_close_request;message_id:"
                                                            + r.getServiceInstance()
                                                            + ";sub_type:Manager_to_agent;source_Service_name:Login;source_Service_instance_network_address:localhost_"
                                                            + r.getPort()
                                                            + ";source_Service_instance_id:"
                                                            + r.getServiceInstance()
                                                            + ";source_plug_name:P;source_plug_port:"
                                                            + r.getPort()
                                                            + ";dest_Service_name:BaaS;dest_Service_instance_network_address:localhost_"
                                                            + b.getPort()
                                                            + ";dest_socket_name:S;dest_socket_port:"
                                                            + b.getPort()
                                                            + ";dest_socket_new_port:l");
                                    b.setConnected(false);
                                }));
                baasListToCloseConnection
                        .forEach(b -> postsConnections
                                .getHistoryList()
                                .stream()
                                // Service needs to close connection with BaaS only if it was connected to it
                                .filter(h -> h.getHistory().stream()
                                        .anyMatch(a -> a.contains("BaaS")))
                                .forEach(r -> {
                                    agentToManagerConnectionThread
                                            .addRequestToAgent(
                                                    "type:source_service_session_close_request;message_id:"
                                                            + r.getServiceInstance()
                                                            + ";sub_type:Manager_to_agent;source_Service_name:Post;source_Service_instance_network_address:localhost_"
                                                            + r.getPort()
                                                            + ";source_Service_instance_id:"
                                                            + r.getServiceInstance()
                                                            + ";source_plug_name:P;source_plug_port:"
                                                            + r.getPort()
                                                            + ";dest_Service_name:BaaS;dest_Service_instance_network_address:localhost_"
                                                            + b.getPort()
                                                            + ";dest_socket_name:S;dest_socket_port:"
                                                            + b.getPort()
                                                            + ";dest_socket_new_port:l");
                                    b.setConnected(false);
                                }));

                baasListToCloseConnection
                        .forEach(b -> registerConnections
                                .getHistoryList()
                                .stream()
                                // Service needs to close connection with BaaS only if it was connected to it
                                .filter(h -> h.getHistory().stream()
                                        .anyMatch(a -> a.contains("BaaS")))
                                .forEach(r -> {
                                    agentToManagerConnectionThread
                                            .addRequestToAgent(
                                                    "type:source_service_session_close_request;message_id:"
                                                            + r.getServiceInstance()
                                                            + ";sub_type:Manager_to_agent;source_Service_name:Register;source_Service_instance_network_address:localhost_"
                                                            + r.getPort()
                                                            + ";source_Service_instance_id:"
                                                            + r.getServiceInstance()
                                                            + ";source_plug_name:P;source_plug_port:"
                                                            + r.getPort()
                                                            + ";dest_Service_name:BaaS;dest_Service_instance_network_address:localhost_"
                                                            + b.getPort()
                                                            + ";dest_socket_name:S;dest_socket_port:"
                                                            + b.getPort()
                                                            + ";dest_socket_new_port:l");
                                    b.setConnected(false);
                                }));
            }

            // if some Services are inactive for 30 seconds Api Gateway needs to close connection with they
            List<ChatHistory> chatListToCloseConnection = chatConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 30 && h.isConnected())
                    .toList();

            // TODO: change static Api Gateway Port and instance for dynamic via Api Gateway History
            if (chatListToCloseConnection.size() > 0) {
                System.out.println("/////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found connection with Chat that is inactive for 30 seconds.");
                System.out.println("/////////////////////////////////////////////");
                chatListToCloseConnection
                        .forEach(c -> {
                            agentToManagerConnectionThread.addRequestToAgent(
                                    "type:source_service_session_close_request;message_id:1;sub_type:Manager_to_agent;source_Service_name:Api Gateway;source_Service_instance_network_address:localhost_34100;source_Service_instance_id:1;source_plug_name:P;source_plug_port:m;dest_Service_name:Chat;dest_Service_instance_network_address:localhost_"
                                            + c.getPort() + ";dest_socket_name:S;dest_socket_port:"
                                            + c.getPort()
                                            + ";dest_socket_new_port:l");
                            c.setConnected(false);
                        });
            }
            List<FileHistory> fileListToCloseConnection = fileConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 30 && h.isConnected())
                    .toList();

            if (fileListToCloseConnection.size() > 0) {
                System.out.println("/////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found connection with File that is inactive for 30 seconds.");
                System.out.println("/////////////////////////////////////////////");
                fileListToCloseConnection
                        .forEach(c -> {
                            agentToManagerConnectionThread.addRequestToAgent(
                                    "type:source_service_session_close_request;message_id:1;sub_type:Manager_to_agent;source_Service_name:Api Gateway;source_Service_instance_network_address:localhost_34100;source_Service_instance_id:1;source_plug_name:P;source_plug_port:m;dest_Service_name:File;dest_Service_instance_network_address:localhost_"
                                            + c.getPort() + ";dest_socket_name:S;dest_socket_port:"
                                            + c.getPort()
                                            + ";dest_socket_new_port:l");
                            c.setConnected(false);
                        });
            }

            List<LoginHistory> loginListToCloseConnection = loginConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 30 && h.isConnected())
                    .toList();

            if (loginListToCloseConnection.size() > 0) {
                System.out.println("/////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found connection with Login that is inactive for 30 seconds.");
                System.out.println("/////////////////////////////////////////////");
                loginListToCloseConnection
                        .forEach(c -> {
                            agentToManagerConnectionThread.addRequestToAgent(
                                    "type:source_service_session_close_request;message_id:1;sub_type:Manager_to_agent;source_Service_name:Api Gateway;source_Service_instance_network_address:localhost_34100;source_Service_instance_id:1;source_plug_name:P;source_plug_port:m;dest_Service_name:Login;dest_Service_instance_network_address:localhost_"
                                            + c.getPort() + ";dest_socket_name:S;dest_socket_port:"
                                            + c.getPort()
                                            + ";dest_socket_new_port:l");
                            c.setConnected(false);
                        });
            }
            List<PostsHistory> postsListToCloseConnection = postsConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 30 && h.isConnected())
                    .toList();

            if (postsListToCloseConnection.size() > 0) {
                System.out.println("/////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found connection with Post that is inactive for 30 seconds.");
                System.out.println("/////////////////////////////////////////////");
                postsListToCloseConnection
                        .forEach(c -> {
                            agentToManagerConnectionThread.addRequestToAgent(
                                    "type:source_service_session_close_request;message_id:1;sub_type:Manager_to_agent;source_Service_name:Api Gateway;source_Service_instance_network_address:localhost_34100;source_Service_instance_id:1;source_plug_name:P;source_plug_port:m;dest_Service_name:Post;dest_Service_instance_network_address:localhost_"
                                            + c.getPort() + ";dest_socket_name:S;dest_socket_port:"
                                            + c.getPort()
                                            + ";dest_socket_new_port:l");
                            c.setConnected(false);
                        });
            }
            List<RegisterHistory> registerListToCloseConnection = registerConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 30 && h.isConnected())
                    .toList();

            if (registerListToCloseConnection.size() > 0) {
                System.out.println("/////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found connection with Register that is inactive for 30 seconds.");
                System.out.println("/////////////////////////////////////////////");
                registerListToCloseConnection
                        .forEach(c -> {
                            agentToManagerConnectionThread.addRequestToAgent(
                                    "type:source_service_session_close_request;message_id:1;sub_type:Manager_to_agent;source_Service_name:Api Gateway;source_Service_instance_network_address:localhost_34100;source_Service_instance_id:1;source_plug_name:P;source_plug_port:m;dest_Service_name:Register;dest_Service_instance_network_address:localhost_"
                                            + c.getPort() + ";dest_socket_name:S;dest_socket_port:"
                                            + c.getPort()
                                            + ";dest_socket_new_port:l");
                            c.setConnected(false);
                        });
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////

            // list of services that are inactive for 1 minute
            List<BaaSHistory> baasListToCloseService = baaSConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 60)
                    .toList();
            // TODO: in AgentToManagerConnectionThread add list of requested services than needs to be closed
            if (baasListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found Baas services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");

                baasListToCloseService
                        .forEach(s -> agentToManagerConnectionThread.addRequestToAgent(
                                "type:graceful_shutdown_request;message_id:" + s.getServiceInstance()
                                        + ";sub_type:Manager_to_agent;Service_name:BaaS;Service_instance_id:"
                                        + s.getServiceInstance() + ""));
                for (BaaSHistory history : baasListToCloseService) {
                    baaSConnections.removeFromHistoryByServiceInstance(
                            history.getServiceInstance());
                }
            }

            List<ChatHistory> chatListToCloseService = chatConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 60)
                    .toList();
            if (chatListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found Chat services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                chatListToCloseService
                        .forEach(s -> agentToManagerConnectionThread.addRequestToAgent(
                                "type:graceful_shutdown_request;message_id:" + s.getServiceInstance()
                                        + ";sub_type:Manager_to_agent;Service_name:Chat;Service_instance_id:"
                                        + s.getServiceInstance() + ""));
                for (ChatHistory history : chatListToCloseService) {
                    chatConnections.removeFromHistoryByServiceInstance(
                            history.getServiceInstance());
                }
            }

            List<FileHistory> fileListToCloseService = fileConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 60)
                    .toList();
            if (fileListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found File services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                fileListToCloseService
                        .forEach(s -> agentToManagerConnectionThread.addRequestToAgent(
                                "type:graceful_shutdown_request;message_id:" + s.getServiceInstance()
                                        + ";sub_type:Manager_to_agent;Service_name:File;Service_instance_id:"
                                        + s.getServiceInstance() + ""));
                for (FileHistory history : fileListToCloseService) {
                    fileConnections.removeFromHistoryByServiceInstance(
                            history.getServiceInstance());
                }
            }

            List<LoginHistory> loginListToCloseService = loginConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 60)
                    .toList();
            if (loginListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found Login services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                loginListToCloseService
                        .forEach(s -> agentToManagerConnectionThread.addRequestToAgent(
                                "type:graceful_shutdown_request;message_id:" + s.getServiceInstance()
                                        + ";sub_type:Manager_to_agent;Service_name:Chat;Service_instance_id:"
                                        + s.getServiceInstance() + ""));
                for (LoginHistory history : loginListToCloseService) {
                    loginConnections.removeFromHistoryByServiceInstance(
                            history.getServiceInstance());
                }
            }

            List<PostsHistory> postListToCloseService = postsConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 60)
                    .toList();
            if (postListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found Posts services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                postListToCloseService
                        .forEach(s -> agentToManagerConnectionThread.addRequestToAgent(
                                "type:graceful_shutdown_request;message_id:" + s.getServiceInstance()
                                        + ";sub_type:Manager_to_agent;Service_name:Chat;Service_instance_id:"
                                        + s.getServiceInstance() + ""));
                for (PostsHistory history : postListToCloseService) {
                    postsConnections.removeFromHistoryByServiceInstance(
                            history.getServiceInstance());
                }
            }

            List<RegisterHistory> registerListToCloseService = registerConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now)
                            .getSeconds()) > 60)
                    .toList();
            if (registerListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println(
                        "Manager -> Found Register services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                registerListToCloseService
                        .forEach(s -> agentToManagerConnectionThread.addRequestToAgent(
                                "type:graceful_shutdown_request;message_id:"
                                        + s.getServiceInstance()
                                        + ";sub_type:Manager_to_agent;Service_name:Register;Service_instance_id:"
                                        + s.getServiceInstance() + ""));
                for (RegisterHistory history : registerListToCloseService) {
                    registerConnections.removeFromHistoryByServiceInstance(
                            history.getServiceInstance());
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Manager Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
