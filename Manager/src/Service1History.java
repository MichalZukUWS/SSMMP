import java.time.LocalDateTime;
import java.util.ArrayList;

public class Service1History {
    private int port;
    private ArrayList<String> history;
    private LocalDateTime lastUsedDateTime;
    private int serviceInstance;

    public Service1History(int port, int serviceInstance) {
        this.port = port;
        this.serviceInstance = serviceInstance;
        history = new ArrayList<>();
    }

    public int getServiceInstance() {
        return serviceInstance;
    }

    public LocalDateTime getLastUsedDateTime() {
        return lastUsedDateTime;
    }

    public void setLastUsedDateTime(LocalDateTime lastUsedDateTime) {
        this.lastUsedDateTime = lastUsedDateTime;
    }

    public int getPort() {
        return port;
    }

    public void addToHistory(String message) {
        history.add(message);
    }

    public ArrayList<String> getHistory() {
        return history;
    }

    public void printHistory() {
        System.out.println("Service1 History:");
        history.forEach(h -> System.out.println(h));
    }

}
