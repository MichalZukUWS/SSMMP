import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class CLI {
    private Socket socket;
    private PrintWriter writerToApiGateway;
    private BufferedReader readerFromApiGateway;
    private int message_id;

    public CLI() {
        message_id = 1000;
    }

    public String convertToProtocole(String dane, int serviceNumber) {
        return "type:Service" + serviceNumber + ";message_id:" + message_id + ";data:" + dane;
    }

    private void clearConsole() {
        try {
            final String os = System.getProperty("os.name");
            ProcessBuilder processBuilder;

            if (os.contains("Windows")) {
                processBuilder = new ProcessBuilder("cmd", "/c", "cls");
            } else {
                processBuilder = new ProcessBuilder("clear");
            }

            Process process = processBuilder.inheritIO().start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() throws UnknownHostException, IOException {
        Scanner sc = new Scanner(System.in);
        String line = null;
        boolean send = true;
        int choice = -1;
        socket = new Socket("localhost", 34100);
        writerToApiGateway = new PrintWriter(socket.getOutputStream());
        readerFromApiGateway = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        while (send) {
            System.out.println(
                    "Provide option:\n" +
                            "1. Sentence of words separated by spaces -> sorted and doubled words.\n" +
                            "2. Word -> converted to uppercase doubled word.\n" +
                            "3. Exit");
            choice = Integer.parseInt(sc.nextLine());
            switch (choice) {
                case 1:
                    System.out.println("Type in words separated by spaces:");
                    line = sc.nextLine();
                    line = convertToProtocole(line, 1);
                    writerToApiGateway.println(line);
                    writerToApiGateway.flush();
                    System.out.println("Response from Api Gateway: " + readerFromApiGateway.readLine());
                    message_id++;
                    System.out.println("Type in anything and press enter to continue");
                    sc.nextLine();
                    clearConsole();
                    break;
                case 2:
                    System.out.println("Type in the word:");
                    line = sc.nextLine();
                    line = convertToProtocole(line, 2);
                    writerToApiGateway.println(line);
                    writerToApiGateway.flush();
                    System.out.println("Response from Api Gateway: " + readerFromApiGateway.readLine());
                    message_id++;
                    System.out.println("Type in anything and press enter to continue");
                    sc.nextLine();
                    clearConsole();
                    break;
                case 3:
                    clearConsole();
                    send = false;
                    break;
                default:
                    System.out.println("You provided the wrong option");
                    break;
            }

        }
        sc.close();
        readerFromApiGateway.close();
        writerToApiGateway.close();
        socket.close();
    }
}
