public class ServiceToAgentMessageWithPort {
    private String message;
    private int message_id;
    private AgentToServiceConnectionThread serviceToAgentConnectionThread;

    public ServiceToAgentMessageWithPort(String message,
            int message_id,
            AgentToServiceConnectionThread serviceToAgentConnectionThread) {

        this.message = message;
        this.message_id = message_id;
        this.serviceToAgentConnectionThread = serviceToAgentConnectionThread;
    }

    public int getMessage_id() {
        return message_id;
    }

    public String getMessage() {
        return message;
    }

    public AgentToServiceConnectionThread getServiceToAgentConnectionThread() {
        return serviceToAgentConnectionThread;
    }
}
