import java.io.IOException;

public class App {
    public static void main(String[] args) throws Exception {
        try {
            new Service().runServer(args[0]);
        } catch (IOException e) {
            System.out.println("Service Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
