import java.io.IOException;

public class App {
    public static void main(String[] args) {
        try {
            new Agent().runServer();
        } catch (IOException e) {
            System.out.println("Agent Exception: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
