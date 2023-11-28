import java.util.ArrayList;

public class Service2Connections {
    private ArrayList<Integer> ports;
    private int numberOfServices;

    public Service2Connections() {
        numberOfServices = 0;
        ports = new ArrayList<>();
    }

    public int getNumberOfServices() {
        return numberOfServices;
    }

    public ArrayList<Integer> getPorts() {
        return ports;
    }

    public void addPort(int port) {
        numberOfServices++;
        ports.add(port);
    }
}
