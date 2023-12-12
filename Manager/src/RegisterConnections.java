import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class RegisterConnections {
    private int numberOfServices;
    private ArrayList<RegisterHistory> historyList;

    public RegisterConnections() {
        numberOfServices = 0;
        historyList = new ArrayList<>();
    }

    public int getNumberOfServices() {
        return numberOfServices;
    }

    public void addNewConnection(RegisterHistory history) {
        numberOfServices++;
        historyList.add(history);
    }

    public void addHistoryByPort(int port, String message) {
        RegisterHistory temp = historyList.stream().filter(s -> s.getPort() == port).findFirst().orElse(null);
        if (temp != null)
            temp.addToHistory(message);
    }

    public boolean isServiceWithPort(int port) {
        return historyList.stream().anyMatch(s -> s.getPort() == port);
    }

    public void updateLastUsedService(int serviceInstance) {
        RegisterHistory temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance).findFirst()
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
            RegisterHistory foo = historyList
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
    }

    public ArrayList<RegisterHistory> getHistoryList() {
        return historyList;
    }

    public int getMaxPort() {
        return historyList.stream().mapToInt(RegisterHistory::getPort).max().orElse(-1);
    }

    public void printHistoryByPort(int port) {
        RegisterHistory temp = historyList.stream().filter(s -> s.getPort() == port).findFirst().orElse(null);
        if (temp != null)
            temp.printHistory();
    }

    public void printHistory() {
        historyList.stream().forEach(s -> s.printHistory());
    }

    public void addHistoryByServiceInstance(int serviceInstance, String message) {
        RegisterHistory temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance).findFirst()
                .orElse(null);
        if (temp != null)
            temp.addToHistory(message);
    }

    public void printHistoryByServiceInstance(int serviceInstance) {
        RegisterHistory temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance).findFirst()
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
