public class RequestForStartService {
    private int messageID;
    private int portOfService;
    private String typeOfService;

    public RequestForStartService(int messageID, int portForService, String typeOfService) {
        this.messageID = messageID;
        this.portOfService = portForService;
        this.typeOfService = typeOfService;
    }

    public int getMessageID() {
        return messageID;
    }

    public String getTypeOfService() {
        return typeOfService;
    }

    public int getPortOfService() {
        return portOfService;
    }

}
