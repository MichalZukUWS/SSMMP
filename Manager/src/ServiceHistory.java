import java.time.LocalDateTime;
import java.util.ArrayList;

public class ServiceHistory {
    private int port;
    private ArrayList<String> history;
    private LocalDateTime lastUsedDateTime;
    private int serviceInstance;
    private boolean isRunning;
    private String name;
    private ArrayList<Plug> plugs;

    public ArrayList<Plug> getPlugs() {
        return plugs;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public ServiceHistory(int port, int serviceInstance, String name) {
        this.port = port;
        this.serviceInstance = serviceInstance;
        history = new ArrayList<>();
        isRunning = false;
        this.name = name;
        plugs = new ArrayList<>();
        lastUsedDateTime = LocalDateTime.now();
        if (this.name.equals("Api_Gateway")) {
            for (int i = 10000; i < 10005; i++) {
                plugs.add(new Plug(i, 1, false, true));
            }
            isRunning = true;
        } else {
            plugs.add(new Plug(port, serviceInstance, false, false));
        }
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
        System.out.println(name + " Data:");
        System.out.print("Port: " + port + ", instance number: " + serviceInstance + ", isRunning: " + isRunning
                + ", lastUsed: " + lastUsedDateTime + ", plugs info: { ");
        plugs.forEach(p -> System.out.print(p.toString()));
        System.out.println(" }");
        System.out.println(name + " History:");
        history.forEach(h -> System.out.println(h));
        System.out.println("////////////////////////////////////////////////////////");
    }

    public void setPlugConnected(int plug, boolean isConnected) {
        plugs
                .stream()
                .filter(p -> p.getPlug() == plug)
                .findFirst()
                .ifPresent(p -> p.setConnected(isConnected));
    }

    public String getName() {
        return name;
    }

    public boolean isPlugInList(int plug) {
        return plugs
                .stream()
                .anyMatch(p -> p.getPlug() == plug);
    }

    public void setPlugLastUsedDateTime(int plug) {
        plugs
                .stream()
                .filter(p -> p.getPlug() == plug)
                .findFirst()
                .ifPresent(p -> p.setLastUsedDateTime(LocalDateTime.now()));
    }
}
