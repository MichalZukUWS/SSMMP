public class ServiceToAgentMessageWithPort {
    private String message;
    private int message_id;
    private ServiceToAgentConnectionThread serviceToAgentConnectionThread;

    public ServiceToAgentMessageWithPort(String message,
            int message_id,
            ServiceToAgentConnectionThread serviceToAgentConnectionThread) {

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

    public ServiceToAgentConnectionThread getServiceToAgentConnectionThread() {
        return serviceToAgentConnectionThread;
    }
}
