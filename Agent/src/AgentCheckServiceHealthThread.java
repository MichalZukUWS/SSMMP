
public class AgentCheckServiceHealthThread extends Thread {
    private ServiceToAgentConnectionThread serviceToAgentConnectionThread;
    private String request;

    // TODO get serviceName and serviceInstance
    public AgentCheckServiceHealthThread(ServiceToAgentConnectionThread serviceToAgentConnectionThread,
            String serviceName, int serviceInstance) {
        this.serviceToAgentConnectionThread = serviceToAgentConnectionThread;
        // TODO what about message_id?
        request = "type:health_control_request;message_id:10;sub_type:agent_to_service_instance;service_name:"
                + serviceName + ";service_instance_id:" + serviceInstance + "";
        start();
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Thread.sleep(60000);
                serviceToAgentConnectionThread.addRequestToService(request);
            } catch (InterruptedException e) {
                System.out.println("Agent Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
