import java.util.ArrayList;

public class AgentCheckServiceHealthThread extends Thread {
    private ArrayList<ServiceConnectionWithAgentEntry> serivcePorts;

    public AgentCheckServiceHealthThread(ArrayList<ServiceConnectionWithAgentEntry> serivcePorts) {
        this.serivcePorts = serivcePorts;
        start();
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Thread.sleep(20000);
                synchronized (serivcePorts) {
                    serivcePorts.forEach(s -> s.getServiceToAgentConnectionThread()
                            .addRequestToService(
                                    "type:health_control_request;message_id:" + s.getServiceInstance()
                                            + ";sub_type:agent_to_service_instance;service_name:"
                                            + s.getTypeOfService() + ";service_instance_id:"
                                            + s.getServiceInstance() + ""));
                    serivcePorts.notify();
                }

            } catch (InterruptedException e) {
                System.out.println("Agent Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
