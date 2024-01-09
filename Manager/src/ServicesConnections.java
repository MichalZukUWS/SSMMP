import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServicesConnections {
    private int numberOfServices;
    private HashMap<String, ArrayList<ServiceHistory>> historyMap;
    private boolean isInitialized;

    public boolean isInitialized() {
        return isInitialized;
    }

    public ServicesConnections() {
        numberOfServices = 0;
        historyMap = new HashMap<>();
        isInitialized = false;
    }

    public void initializeConnection(String[] array) {
        for (int i = 0; i < array.length; i++) {
            historyMap.put(array[i], new ArrayList<>());
        }
        isInitialized = true;
    }

    public List<Plug> getAllPlugs() {
        return historyMap.values().stream()
                .flatMap(List::stream)
                .flatMap(serviceHistory -> serviceHistory.getPlugs().stream())
                .collect(Collectors.toList());
    }

    public ArrayList<ServiceHistory> getHistoryList(String name) {
        return historyMap.get(name);
    }

    public Collection<ArrayList<ServiceHistory>> getHistoryList() {
        return historyMap.values();
    }

    public int getNumberOfServices(String name) {
        var history = historyMap.get(name);
        if (history != null) {
            return history.size();
        }
        return -1;
    }

    public boolean isServiceTypeInConnections(String name) {
        return historyMap.containsKey(name);
    }

    public int getNumberOfServices() {
        return numberOfServices;
    }

    public void addNewConnection(String name, ServiceHistory historyEntry) {
        var history = historyMap.get(name);
        if (history != null) {
            history.add(historyEntry);
            numberOfServices++;
        } else {
            System.out.println("Service: " + name + " isn't in connection list");
        }
    }

    public void addHistoryByPort(int port, String message) {
        historyMap.values()
                .forEach(
                        s -> s
                                .stream()
                                .filter(h -> h.getPort() == port)
                                .findFirst()
                                .ifPresent(f -> f.addToHistory(message)));
    }

    public boolean isServiceWithPort(int port) {
        return historyMap.values()
                .stream()
                .anyMatch(s -> s
                        .stream()
                        .anyMatch(f -> f.getPort() == port));

    }

    public boolean isServiceWithNumberOfInstance(int serviceInstance) {
        return historyMap.values()
                .stream()
                .anyMatch(s -> s
                        .stream()
                        .anyMatch(f -> f.getServiceInstance() == serviceInstance));
    }

    public boolean isServiceWithPlug(int plug) {
        return historyMap.values()
                .stream()
                .anyMatch(s -> s
                        .stream()
                        .anyMatch(p -> p.isPlugInList(plug)));
    }

    public void updateLastUsedService(int serviceInstance) {

        historyMap.values()
                .forEach(h -> h
                        .stream()
                        .filter(f -> f
                                .getServiceInstance() == serviceInstance)
                        .findFirst()
                        .ifPresent(g -> g.setLastUsedDateTime(LocalDateTime.now())));

    }

    public void updateLastUsedPlug(int plug) {
        historyMap.values()
                .forEach(h -> h
                        .stream()
                        .filter(f -> f.isPlugInList(plug))
                        .findFirst()
                        .ifPresent(s -> s.setPlugLastUsedDateTime(plug)));
    }

    public int getPort(String name) {

        // go via historyList and find that object which have latest value in lastUsedDateTime field
        var history = historyMap.get(name);

        if (history != null) {
            Optional<LocalDateTime> temp = history
                    .stream()
                    .filter(s -> s.isRunning())
                    .map(k -> k.getLastUsedDateTime())
                    .max(Comparator.naturalOrder());
            if (temp.isPresent()) {
                Optional<ServiceHistory> foo = history
                        .stream()
                        .filter(
                                s -> s.getLastUsedDateTime().equals(temp.get()))
                        .findFirst();
                if (foo.isPresent()) {
                    return foo.get().getPort();
                }
            }
        }

        return -1;
    }

    public int getMaxPort() {
        int max = -1;
        for (var history : historyMap.values()) {
            int temp = history
                    .stream()
                    .mapToInt(ServiceHistory::getPort)
                    .max()
                    .orElse(-1);
            if (temp > max)
                max = temp;
        }
        return max;
    }

    public void printHistoryByPort(int port) {
        historyMap.values()
                .forEach(
                        s -> s
                                .stream()
                                .filter(
                                        f -> f.getPort() == port)
                                .findFirst().ifPresent(e -> e.printHistory()));
    }

    public void printHistory() {
        historyMap.forEach((k, v) -> v.forEach(h -> h.printHistory()));
    }

    public void addHistoryByServiceInstance(int serviceInstance, String message) {

        historyMap.values()
                .forEach(s -> s
                        .stream()
                        .filter(h -> h.getServiceInstance() == serviceInstance)
                        .findFirst()
                        .ifPresent(e -> e.addToHistory(message)));

    }

    public void printHistoryByServiceInstance(int serviceInstance) {

        historyMap.values()
                .forEach(h -> h
                        .stream()
                        .filter(s -> s.getServiceInstance() == serviceInstance)
                        .findFirst()
                        .ifPresent(e -> e.printHistory()));

    }

    public void removeFromHistoryByPort(int port) {

        for (var history : historyMap.values()) {
            if (history.removeIf(h -> h.getPort() == port)) {
                numberOfServices--;
            }
        }
    }

    public void removeFromHistoryByServiceInstance(int serviceInstance) {
        for (var history : historyMap.values()) {
            if (history.removeIf(h -> h.getServiceInstance() == serviceInstance)) {
                numberOfServices--;
            }
        }
    }

    public void setRunningByPort(int port, boolean isRunning) {

        historyMap.values()
                .forEach(s -> s
                        .stream()
                        .filter(h -> h.getPort() == port)
                        .findFirst()
                        .ifPresent(e -> e.setRunning(isRunning)));

    }

    public void setRunningByServiceInstance(int serviceInstance, boolean isRunning) {

        historyMap.values()
                .forEach(s -> s
                        .stream()
                        .filter(h -> h.getServiceInstance() == serviceInstance)
                        .findFirst()
                        .ifPresent(e -> e.setRunning(isRunning)));

    }

    public String getTypeOfServiceByPort(int port) {
        for (var history : historyMap.entrySet()) {
            var name = history.getValue()
                    .stream()
                    .filter(h -> h.getPort() == port)
                    .findFirst();
            if (name.isPresent()) {
                return history.getKey();
            }
        }
        return null;
    }

    public String getTypeOfServiceByNumberOfInstance(int serviceInstance) {
        for (var history : historyMap.entrySet()) {
            var name = history.getValue()
                    .stream()
                    .filter(h -> h.getServiceInstance() == serviceInstance)
                    .findFirst();
            if (name.isPresent()) {
                return history.getKey();
            }
        }
        return null;
    }

    public String getTypeOfServiceByPlug(int plug) {
        for (var history : historyMap.entrySet()) {
            var name = history.getValue()
                    .stream()
                    .filter(h -> h.getPlugs()
                            .stream()
                            .anyMatch(p -> p.getPlug() == plug))
                    .findFirst();
            if (name.isPresent()) {
                return history.getKey();
            }
        }
        return null;
    }

    public void setPlugIsConneted(int plug, boolean isConnected) {
        historyMap.values()
                .forEach(h -> h
                        .forEach(f -> f.getPlugs()
                                .stream()
                                .filter(p -> p.getPlug() == plug)
                                .findFirst()
                                .ifPresent(e -> e.setConnected(isConnected))));
    }
}
