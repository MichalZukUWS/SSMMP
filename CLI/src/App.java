import java.io.IOException;
import java.net.UnknownHostException;

public class App {
    public static void main(String[] args) {
        // new CLI(args[0]).run();
        try {
            new CLI().run();
        } catch (UnknownHostException e) {
            System.out.println("CLI Exception: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("CLI Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}