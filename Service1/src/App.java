import java.io.IOException;

public class App {
    public static void main(String[] args) throws Exception {
        try {
            new Service1().runServer(args[0]);
        } catch (IOException e) {
            System.out.println("Service1 Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
