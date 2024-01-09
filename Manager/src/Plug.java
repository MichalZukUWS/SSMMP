import java.time.LocalDateTime;

public class Plug {
    private int plug;
    private boolean isConnected;
    private LocalDateTime lastUsedDateTime;
    private boolean isOut;
    private int serviceInstance;

    public Plug(int plug, int serviceInstance, boolean isConnected, boolean isOut) {
        this.plug = plug;
        this.serviceInstance = serviceInstance;
        this.isConnected = isConnected;
        this.isOut = isOut;
        lastUsedDateTime = LocalDateTime.now();
    }

    public int getServiceInstance() {
        return serviceInstance;
    }

    public boolean isOut() {
        return isOut;
    }

    public LocalDateTime getLastUsedDateTime() {
        return lastUsedDateTime;
    }

    public void setLastUsedDateTime(LocalDateTime lastUsedDateTime) {
        this.lastUsedDateTime = lastUsedDateTime;
    }

    public int getPlug() {
        return plug;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    @Override
    public String toString() {
        return " Plug [plug=" + plug + ", isConnected=" + isConnected + ", lastUsedDateTime=" + lastUsedDateTime
                + ", isOut=" + isOut + ", serviceInstance=" + serviceInstance + "] ";
    }

}
