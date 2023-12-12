import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class FileConnections {
    private int numberOfServices;
    private ArrayList<FileHistory> historyList;

    public FileConnections() {
        numberOfServices = 0;
        historyList = new ArrayList<>();
    }

    public int getNumberOfServices() {
        return numberOfServices;
    }

    public void addNewConnection(FileHistory history) {
        numberOfServices++;
        historyList.add(history);
    }

    public void addHistoryByPort(int port, String message) {
        FileHistory temp = historyList.stream().filter(s -> s.getPort() == port).findFirst().orElse(null);
        if (temp != null)
            temp.addToHistory(message);
    }

    public boolean isServiceWithPort(int port) {
        return historyList.stream().anyMatch(s -> s.getPort() == port);
    }

    public void updateLastUsedService(int serviceInstance) {
        FileHistory temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance).findFirst()
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
            FileHistory foo = historyList
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

    public ArrayList<FileHistory> getHistoryList() {
        return historyList;
    }

    public int getMaxPort() {
        return historyList.stream().mapToInt(FileHistory::getPort).max().orElse(-1);
    }

    public void printHistoryByPort(int port) {
        FileHistory temp = historyList.stream().filter(s -> s.getPort() == port).findFirst().orElse(null);
        if (temp != null)
            temp.printHistory();
    }

    public void printHistory() {
        historyList.stream().forEach(s -> s.printHistory());
    }

    public void addHistoryByServiceInstance(int serviceInstance, String message) {
        FileHistory temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance).findFirst()
                .orElse(null);
        if (temp != null)
            temp.addToHistory(message);
    }

    public void printHistoryByServiceInstance(int serviceInstance) {
        FileHistory temp = historyList.stream().filter(s -> s.getServiceInstance() == serviceInstance).findFirst()
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