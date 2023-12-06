import java.util.ArrayList;

public class Service1Connections {
    // private ArrayList<Integer> ports;
    private int numberOfServices;
    private ArrayList<Service1History> historyList;

    public Service1Connections() {
        numberOfServices = 0;
        historyList = new ArrayList<>();
        // ports = new ArrayList<>();
    }

    public int getNumberOfServices() {
        return numberOfServices;
    }

    // public ArrayList<Integer> getPorts() {
    // return ports;
    // }

    // public void addPort(int port) {
    // numberOfServices++;
    // ports.add(port);
    // }

    public void addNewConnection(Service1History history) {
        numberOfServices++;
        historyList.add(history);
    }

    public void addHistoryByPort(int port, String message) {
        Service1History temp = historyList.stream().filter(s -> s.getPort() == port).findFirst().orElse(null);
        if (temp != null)
            temp.addToHistory(message);
    }

    public boolean isServiceWithPort(int port) {
        return historyList.stream().anyMatch(s -> s.getPort() == port);
    }

    public int getPort() {
        // TODO change to latest used service/least used service
        return historyList.get(0).getPort();
    }

    public int getMaxPort() {
        return historyList.stream().mapToInt(Service1History::getPort).max().orElse(-1);
    }

    public void printHistoryByPort(int port) {
        Service1History temp = historyList.stream().filter(s -> s.getPort() == port).findFirst().orElse(null);
        if (temp != null)
            temp.printHistory();
    }

    public void printHistory() {
        historyList.stream().forEach(s -> s.printHistory());
    }
}
