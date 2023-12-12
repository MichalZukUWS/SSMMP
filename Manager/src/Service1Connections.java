import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Service1Connections {
    private int numberOfServices;
    private ArrayList<Service1History> historyList;

    public Service1Connections() {
        numberOfServices = 0;
        historyList = new ArrayList<>();
    }

    public int getNumberOfServices() {
        return numberOfServices;
    }

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

    public void updateLastUsedService(int serviceInstance) {
        Service1History temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance).findFirst()
                .orElse(null);
        if (temp != null) {
            temp.setLastUsedDateTime(LocalDateTime.now());
        }
    }

    public int getPort() {

        // go via historyList and find that object which have latest value in
        // lastUsedDateTime field
        LocalDateTime temp = historyList.stream().map(k -> k.getLastUsedDateTime()).max(Comparator.naturalOrder())
                .orElse(null);
        if (temp != null) {
            Service1History foo = historyList
                    .stream()
                    .filter(
                            s -> s.getLastUsedDateTime().equals(temp))
                    .findFirst()
                    .orElse(null);
            if (foo != null) {
                return foo.getPort();
            }
        }

        return -1;

        // LocalDateTime maxDate = historyList.stream()
        // .map(s -> s.getLastUsedDateTime())
        // .max(Comparator.naturalOrder())
        // .orElse(null);
        // if (maxDate != null) {
        // Service1History foo = historyList.stream().filter(s ->
        // s.getLastUsedDateTime().equals(maxDate)).findFirst()
        // .orElse(null);
        // return foo.getPort();
        // }
        // return -1;
    }

    public ArrayList<Service1History> getHistoryList() {
        return historyList;
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

    public void addHistoryByServiceInstance(int serviceInstance, String message) {
        Service1History temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance).findFirst()
                .orElse(null);
        if (temp != null)
            temp.addToHistory(message);
    }

    public void printHistoryByServiceInstance(int serviceInstance) {
        Service1History temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance).findFirst()
                .orElse(null);
        if (temp != null)
            temp.printHistory();
    }

    // TODO:: change to instance
    public void removeFromHistoryByPort(int port) {
        historyList = historyList.stream().filter(h -> h.getPort() != port)
                .collect(Collectors.toCollection(ArrayList::new));

        numberOfServices = historyList.size();
    }
}
