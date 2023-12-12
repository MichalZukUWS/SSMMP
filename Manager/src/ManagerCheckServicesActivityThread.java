import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class ManagerCheckServicesActivityThread extends Thread {

    // TODO: add reference to AgentToManagerConnectioThread and create method that
    // provide request to send to Agent
    private BaaSConnections baaSConnections;
    private ChatConnections chatConnections;
    private FileConnections fileConnections;
    private LoginConnections loginConnections;
    private PostsConnections postsConnections;
    private RegisterConnections registerConnections;
    private LocalDateTime now;
    private LinkedList<String> requests;

    public ManagerCheckServicesActivityThread(BaaSConnections baaSConnections, ChatConnections chatConnections,
            FileConnections fileConnections, LoginConnections loginConnections, PostsConnections postsConnections,
            RegisterConnections registerConnections, LinkedList<String> requests) {
        this.baaSConnections = baaSConnections;
        this.chatConnections = chatConnections;
        this.fileConnections = fileConnections;
        this.postsConnections = postsConnections;
        this.registerConnections = registerConnections;
        this.baaSConnections = baaSConnections;
        this.loginConnections = loginConnections;
        this.requests = requests;
        start();
    }

    public void run() {
        while (!isInterrupted()) {
            now = LocalDateTime.now();

            // list of services that are innactive for 1 minutes

            // ArrayList<Service1History> service1ListToCloseConnection =
            // (ArrayList<Service1History>) service1Connections
            // .getHistoryList()
            // .stream()
            // .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(),
            // now).getSeconds()) > 60)
            // .toList();

            // TODO: forEach and add request to close connection from Api Gateway to
            // Service1

            // list of services that are innactive for 2 minutes
            // TODO: change to 120 seconds in release version
            // in develop stage is 30 seconds
            List<BaaSHistory> baasListToCloseService = baaSConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now).getSeconds()) > 30)
                    .toList();
            // TODO: in AgentToManagerConnectionThread add method that add request to
            // requests list
            // TODO: what about message_id?
            // TODO: change s.getPort() to: s.getServiceInstance() and add in Agent project
            // TODO: in AgentToManagerConnectionThread add list of requested services than
            // needs to be closed
            // information about service instance
            if (baasListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println("Manager -> Found Baas services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                synchronized (requests) {
                    baasListToCloseService.forEach(s -> requests.add(
                            "type:graceful_shutdown_request;message_id:50;sub_type:Manager_to_agent;Service_name:BaaS;Service_instance_id:"
                                    + s.getPort() + ""));
                    for (BaaSHistory history : baasListToCloseService) {
                        baaSConnections.removeFromHistoryByPort(history.getPort());
                    }
                    requests.notify();
                }
            }

            List<ChatHistory> chatListToCloseService = chatConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now).getSeconds()) > 30)
                    .toList();
            if (chatListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println("Manager -> Found Chat services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                synchronized (requests) {
                    chatListToCloseService.forEach(s -> requests.add(
                            "type:graceful_shutdown_request;message_id:50;sub_type:Manager_to_agent;Service_name:Chat;Service_instance_id:"
                                    + s.getPort() + ""));
                    for (ChatHistory history : chatListToCloseService) {
                        chatConnections.removeFromHistoryByPort(history.getPort());
                    }
                    requests.notify();
                }
            }

            List<FileHistory> fileListToCloseService = fileConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now).getSeconds()) > 30)
                    .toList();
            if (fileListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println("Manager -> Found File services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                synchronized (requests) {
                    fileListToCloseService.forEach(s -> requests.add(
                            "type:graceful_shutdown_request;message_id:50;sub_type:Manager_to_agent;Service_name:File;Service_instance_id:"
                                    + s.getPort() + ""));
                    for (FileHistory history : fileListToCloseService) {
                        fileConnections.removeFromHistoryByPort(history.getPort());
                    }
                    requests.notify();
                }
            }

            List<LoginHistory> loginListToCloseService = loginConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now).getSeconds()) > 30)
                    .toList();
            if (loginListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println("Manager -> Found Login services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                synchronized (requests) {
                    loginListToCloseService.forEach(s -> requests.add(
                            "type:graceful_shutdown_request;message_id:50;sub_type:Manager_to_agent;Service_name:Chat;Service_instance_id:"
                                    + s.getPort() + ""));
                    for (LoginHistory history : loginListToCloseService) {
                        loginConnections.removeFromHistoryByPort(history.getPort());
                    }
                    requests.notify();
                }
            }

            List<PostsHistory> postListToCloseService = postsConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now).getSeconds()) > 30)
                    .toList();
            if (postListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println("Manager -> Found Posts services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                synchronized (requests) {
                    postListToCloseService.forEach(s -> requests.add(
                            "type:graceful_shutdown_request;message_id:50;sub_type:Manager_to_agent;Service_name:Chat;Service_instance_id:"
                                    + s.getPort() + ""));
                    for (PostsHistory history : postListToCloseService) {
                        postsConnections.removeFromHistoryByPort(history.getPort());
                    }
                    requests.notify();
                }
            }

            List<RegisterHistory> registerListToCloseService = registerConnections
                    .getHistoryList()
                    .stream()
                    .filter(h -> Math.abs(Duration.between(h.getLastUsedDateTime(), now).getSeconds()) > 30)
                    .toList();
            if (registerListToCloseService.size() > 0) {
                System.out.println("////////////////////////////////////////////////////////");
                System.out.println("Manager -> Found Register services that I will close due to inactivity");
                System.out.println("////////////////////////////////////////////////////////");
                synchronized (requests) {
                    registerListToCloseService.forEach(s -> requests.add(
                            "type:graceful_shutdown_request;message_id:50;sub_type:Manager_to_agent;Service_name:Register;Service_instance_id:"
                                    + s.getPort() + ""));
                    for (RegisterHistory history : registerListToCloseService) {
                        registerConnections.removeFromHistoryByPort(history.getPort());
                    }
                    requests.notify();
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
