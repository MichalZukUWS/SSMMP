public class ManagerCheckServicesActivityThread extends Thread {

    private Service1Connections service1Connections;
    private Service2Connections service2Connections;
    private BaaSConnections baaSConnections;

    public ManagerCheckServicesActivityThread(Service1Connections service1Connections,
            Service2Connections service2Connections, BaaSConnections baaSConnections) {
        this.service1Connections = service1Connections;
        this.service2Connections = service2Connections;
        this.baaSConnections = baaSConnections;
        start();
    }

    public void run() {
        while (!isInterrupted()) {
            // TODO implement a mechanism for checking the activity of services
        }
    }

}
