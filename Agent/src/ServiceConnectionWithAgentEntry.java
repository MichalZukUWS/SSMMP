public class ServiceConnectionWithAgentEntry {
    private int port;
    private int message_id;
    private AgentToServiceConnectionThread serviceToAgentConnectionThread;
    private int serviceInstance;
    private String typeOfService;

    public ServiceConnectionWithAgentEntry(int port, int serviceInstance, int message_id, String typeOfService,
            AgentToServiceConnectionThread serviceToAgentConnectionThread) {
        this.port = port;
        this.message_id = message_id;
        this.serviceToAgentConnectionThread = serviceToAgentConnectionThread;
        this.serviceInstance = serviceInstance;
        this.typeOfService = typeOfService;
    }

    public String getTypeOfService() {
        return typeOfService;
    }

    public void setTypeOfService(String typeOfService) {
        this.typeOfService = typeOfService;
    }

    public int getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(int serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMessage_id() {
        return message_id;
    }

    public void setMessage_id(int message_id) {
        this.message_id = message_id;
    }

    public AgentToServiceConnectionThread getServiceToAgentConnectionThread() {
        return serviceToAgentConnectionThread;
    }

    public void setServiceToAgentConnectionThread(AgentToServiceConnectionThread serviceToAgentConnectionThread) {
        this.serviceToAgentConnectionThread = serviceToAgentConnectionThread;
    }

}
