import java.io.IOException;

public class App {
    public static void main(String[] args) {
        try {
            new Manager().runServer();
        } catch (IOException e) {
            System.out.println("Manager Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
