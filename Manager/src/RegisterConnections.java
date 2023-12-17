import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

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
        Optional<RegisterHistory> temp = historyList.stream().filter(s -> s.getPort() == port).findFirst();
        if (temp.isPresent())
            temp.get().addToHistory(message);
    }

    public boolean isServiceWithPort(int port) {
        return historyList.stream().anyMatch(s -> s.getPort() == port);
    }

    public void updateLastUsedService(int serviceInstance) {
        Optional<RegisterHistory> temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance)
                .findFirst();
        if (temp.isPresent()) {
            temp.get().setLastUsedDateTime(LocalDateTime.now());
        }
    }

    public int getPort() {

        // go via historyList and find that object which have latest value in
        // lastUsedDateTime field
        Optional<LocalDateTime> temp = historyList.stream().map(k -> k.getLastUsedDateTime())
                .max(Comparator.naturalOrder());

        if (temp.isPresent()) {
            Optional<RegisterHistory> foo = historyList
                    .stream()
                    .filter(
                            s -> s.getLastUsedDateTime().equals(temp.get()))
                    .findFirst();

            if (foo != null) {
                return foo.get().getPort();
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
        Optional<RegisterHistory> temp = historyList.stream().filter(s -> s.getPort() == port).findFirst();
        if (temp.isPresent())
            temp.get().printHistory();
    }

    public void printHistory() {
        historyList.stream().forEach(s -> s.printHistory());
    }

    public void addHistoryByServiceInstance(int serviceInstance, String message) {
        Optional<RegisterHistory> temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance)
                .findFirst();

        if (temp.isPresent())
            temp.get().addToHistory(message);
    }

    public void printHistoryByServiceInstance(int serviceInstance) {
        Optional<RegisterHistory> temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance)
                .findFirst();

        if (temp.isPresent())
            temp.get().printHistory();
    }

    public void removeFromHistoryByPort(int port) {
        historyList.removeIf(h -> h.getPort() == port);

        numberOfServices = historyList.size();
    }

    public void removeFromHistoryByServiceInstance(int serviceInstance) {
        historyList.removeIf(h -> h.getServiceInstance() == serviceInstance);

        numberOfServices = historyList.size();
    }

    public void setConnectedByPort(int port, boolean isConnected) {
        Optional<RegisterHistory> temp = historyList.stream().filter(h -> h.getPort() == port).findFirst();
        if (temp.isPresent()) {
            temp.get().setConnected(isConnected);
        }

    }
}
