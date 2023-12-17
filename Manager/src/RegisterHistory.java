import java.time.LocalDateTime;
import java.util.ArrayList;

public class RegisterHistory {
    private int port;
    private ArrayList<String> history;
    private LocalDateTime lastUsedDateTime;
    private int serviceInstance;
    private boolean isConnected;

    public RegisterHistory(int port, int serviceInstance) {
        this.port = port;
        this.serviceInstance = serviceInstance;
        history = new ArrayList<>();
        isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
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
        System.out.println("Register History:");
        history.forEach(h -> System.out.println(h));
        System.out.println("////////////////////////////////////////////////////////");
    }
}
