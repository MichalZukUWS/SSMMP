import java.time.LocalDateTime;
import java.util.ArrayList;

public class BaaSHistory {
    private int port;
    private ArrayList<String> history;
    private LocalDateTime lastUsedDateTime;
    private int serviceInstance;
    private boolean isConnected;

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public BaaSHistory(int port, int serviceInstance) {
        this.port = port;
        this.serviceInstance = serviceInstance;
        history = new ArrayList<>();
        isConnected = false;
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
        System.out.println("\n////////////////////////////////////////////////////////");
        System.out.println("BaaS History:");
        history.forEach(h -> System.out.println(h));
        System.out.println("////////////////////////////////////////////////////////");
    }
}
