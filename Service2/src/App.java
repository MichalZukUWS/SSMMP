import java.io.IOException;

public class App {
    public static void main(String[] args) throws Exception {
        try {
            new Service2().runServer(args[0]);
        } catch (IOException e) {
            System.out.println("Service2 Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
