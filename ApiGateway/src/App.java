import java.io.IOException;

public class App {
    public static void main(String[] args) throws Exception {
        try {
            new ApiGateway().runServer();
        } catch (IOException e) {
            System.out.println("Api Gateway Exception: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
