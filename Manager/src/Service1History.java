import java.util.ArrayList;

// TODO add a mechanism responsible for service activity
public class Service1History {
    private int port;
    private ArrayList<String> history;

    public Service1History(int port) {
        this.port = port;
        history = new ArrayList<>();
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
