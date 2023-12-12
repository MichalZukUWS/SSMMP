import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ManagerCheckServicesActivityThread extends Thread {

    // TODO: add reference to AgentToManagerConnectioThread and create method that
    // provide request to send to Agent
    private Service1Connections service1Connections;
    private Service2Connections service2Connections;
    private BaaSConnections baaSConnections;
    private LocalDateTime now;
    private LinkedList<String> requests;

    public ManagerCheckServicesActivityThread(Service1Connections service1Connections,
            Service2Connections service2Connections, BaaSConnections baaSConnections, LinkedList<String> requests) {
        this.service1Connections = service1Connections;
        this.service2Connections = service2Connections;
        this.baaSConnections = baaSConnections;
        this.requests = requests;
        start();
    }

    public void run() {
        while (!isInterrupted()) {
            // TODO: implement a mechanism for checking the activity of Services
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
            List<Service1History> service1ListToCloseService = service1Connections
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
            if (service1ListToCloseService.size() > 0) {
                System.out.println("Manager -> Found services(Service1) that I will close due to inactivity");
                synchronized (requests) {
                    service1ListToCloseService.forEach(s -> requests.add(
                            "type:graceful_shutdown_request;message_id:50;sub_type:Manager_to_agent;Service_name:Service1;Service_instance_id:"
                                    + s.getPort() + ""));
                    for (Service1History history : service1ListToCloseService) {
                        service1Connections.removeFromHistoryByPort(history.getPort());
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
