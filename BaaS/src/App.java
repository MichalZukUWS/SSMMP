import java.io.IOException;

public class App {
    public static void main(String[] args) throws Exception {
        try {
            new BaaS().runServer(args[0]);
        } catch (IOException e) {
            System.out.println("** BaaS -> ** Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
