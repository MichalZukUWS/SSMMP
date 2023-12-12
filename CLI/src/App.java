import java.io.IOException;
import java.net.UnknownHostException;

public class App {
    public static void main(String[] args) {
        try {
            new CLI();
        } catch (UnknownHostException e) {
            System.out.println("CLI Exception: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("CLI Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}