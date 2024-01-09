import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class ManagerCheckServicesActivityThread extends Thread {
    private ServicesConnections connections;
    private AgentToManagerConnectionThread agentToManagerConnectionThread;
    private LocalDateTime now;
    private HashMap<Integer, Integer> plugsToClose;
    private ArrayList<Integer> serviceInstancesToClose;

    public ManagerCheckServicesActivityThread(ServicesConnections connections, HashMap<Integer, Integer> plugsToClose,
            ArrayList<Integer> serviceInstancesToClose,
            AgentToManagerConnectionThread agentToManagerConnectionThread) {
        this.connections = connections;
        this.agentToManagerConnectionThread = agentToManagerConnectionThread;
        this.plugsToClose = plugsToClose;
        this.serviceInstancesToClose = serviceInstancesToClose;
        start();
    }

    public void run() {
        while (!isInterrupted()) {
            now = LocalDateTime.now();
            synchronized (connections) {
                synchronized (plugsToClose) {
                    if (connections.isInitialized()) {
                        connections.getAllPlugs()
                                .stream()
                                .filter(s -> Math
                                        .abs(Duration.between(s.getLastUsedDateTime(), now).getSeconds()) > 30
                                        && s.isConnected() && s.isOut()
                                        && !plugsToClose.values().contains(s.getPlug()))
                                .forEach(g -> {
                                    plugsToClose.put(g.getServiceInstance(), g.getPlug());
                                    System.out.println("Manager -> Founded plug/s to close.");
                                    agentToManagerConnectionThread.addRequestToAgent(
                                            "type:source_service_session_close_request;message_id:"
                                                    + g.getServiceInstance()
                                                    + ";sub_type:Manager_to_agent;source_Service_name:A;source_Service_instance_network_address:localhost;source_Service_instance_id:"
                                                    + g.getServiceInstance()
                                                    + ";source_plug_name:P;source_plug_port:"
                                                    + g.getPlug()
                                                    + ";dest_Service_name:B;dest_Service_instance_network_address:NA_j;dest_socket_name:S;dest_socket_port:k;dest_socket_new_port:l");
                                });
                        plugsToClose.notify();
                    }
                    synchronized (serviceInstancesToClose) {
                        connections.getHistoryList()
                                .forEach(g -> g
                                        .stream()
                                        .filter(k -> !k.getName().equals("Api_Gateway")
                                                && Math.abs(Duration.between(k.getLastUsedDateTime(), now)
                                                        .getSeconds()) > 60
                                                && k.isRunning()
                                                && !serviceInstancesToClose.contains(k.getServiceInstance()))
                                        .forEach(f -> {
                                            System.out.println("Manager -> Founded services to close.");
                                            agentToManagerConnectionThread.addRequestToAgent(
                                                    "type:graceful_shutdown_request;message_id:"
                                                            + f.getServiceInstance()
                                                            + ";sub_type:Manager_to_agent;Service_name:A;Service_instance_id:"
                                                            + f.getServiceInstance() + "");
                                            serviceInstancesToClose.add(f.getServiceInstance());
                                        }));
                        serviceInstancesToClose.notify();
                    }
                }
                connections.notify();
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
