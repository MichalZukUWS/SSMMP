import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Scanner;

class CLI {

    private String commands;
    private Socket socketToApiGateway;
    private BufferedReader readerFromApiGateway;
    private PrintWriter writerToApiGateway;

    public CLI() throws UnknownHostException, IOException {
        commands = "Available commands:\n1. Registering a new user\n2. Login\n3. Displaying the last 10 posts\n4. Adding posts\n5. Sending files to the cloud\n6. Downloading files from the cloud\n7. Logout\n8. Help\n9. Exit";
        run();
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

        boolean logged = false;
        int message_id = 1000;

        socketToApiGateway = new Socket("localhost", 34100);
        writerToApiGateway = new PrintWriter(socketToApiGateway.getOutputStream(), true);
        readerFromApiGateway = new BufferedReader(
                new InputStreamReader(socketToApiGateway.getInputStream()));

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String message = null;
        int choice = -1;
        String login = null;
        String password = null;
        String choiceString = null;
        while (choice != 9) {
            System.out.println(commands);
            message = null;
            System.out.println("Provide option:");
            choiceString = reader.readLine();
            choice = Integer.parseInt(choiceString);
            switch (choice) {
                case 1:
                    System.out.println("Enter login: ");
                    login = reader.readLine();
                    System.out.println("Enter password(min 8 characters):");
                    password = reader.readLine();
                    message = "type:register_request;message_id:" + message_id + ";data:" + login + ";data:"
                            + password
                            + "";
                    break;
                case 2:
                    System.out.println("Enter login: ");
                    login = reader.readLine();
                    System.out.println("Enter password(min 8 characters):");
                    password = reader.readLine();
                    message = "type:login_request;message_id:" + message_id + ";data:" + login + ";data:" + password
                            + "";
                    break;
                case 3:
                    if (logged) {
                        message = "type:display_posts_request;message_id:" + message_id + "";
                    } else {
                        System.out.println("You need to login first.");
                    }
                    break;
                case 4:
                    if (logged) {
                        message = "type:chat_request;message_id:" + message_id + "";
                    } else {
                        System.out.println("Yoo need to login first.");
                    }
                    break;
                case 5:
                    if (logged) {

                        String filePath = "file.txt";
                        File file = new File(filePath);
                        FileInputStream inputStream = new FileInputStream(file);

                        byte[] bytes = new byte[(int) file.length()];
                        inputStream.read(bytes);
                        inputStream.close();

                        String base64EncodedContent = Base64.getEncoder().encodeToString(bytes);

                        message = "type:file_upload_request;message_id:" + message_id + ";data:" + filePath + ";data:"
                                + base64EncodedContent + "";
                    } else {
                        System.out.println("Yoo need to login first.");
                    }
                    break;
                case 6:
                    if (logged) {
                        message = "type:file_download_request;message_id:" + message_id + "";
                    } else {
                        System.out.println("Yoo need to login first.");
                    }
                    break;
                case 7:
                    System.out.println("You logged off.");
                    logged = false;
                    break;
                case 8:
                    System.out.println(commands);
                    break;
                case 9:
                    System.out.println("Closing program.");
                    break;

                default:
                    System.out.println("Provided wrong option.");
                    break;
            }

            if (message != null) {
                writerToApiGateway.println(message);
                writerToApiGateway.flush();

                String response = readerFromApiGateway.readLine();
                System.out.println("CLI got response: " + response);
                String[] decodedData = response.split(";");
                int status = Integer.parseInt(decodedData[2].split(":")[1]);
                switch (decodedData[0].split(":")[1]) {
                    case "register_response":
                        if (status < 300) {
                            System.out.println("Registraion was successfull as: " + login + ", you can login now.");
                        } else {
                            System.out.println(decodedData[3].split(":")[1]);
                        }
                        break;

                    case "login_response":
                        if (status < 300) {
                            System.out.println("You successfully logged as: " + login);
                            logged = true;
                        } else {
                            System.out.println(decodedData[3].split(":")[1]);
                        }
                        break;

                    case "display_posts_response":
                        if (status < 300) {
                            System.out.println("10 last posts:");
                            for (int i = 3; i < decodedData.length; i++) {
                                System.out.println(decodedData[i].split(":")[1]);
                            }
                        } else {
                            System.out.println(decodedData[3].split(":")[1]);
                        }
                        break;

                    case "chat_response":
                        if (status < 300) {
                            System.out.println("You successfully post message.");
                        } else {
                            System.out.println(decodedData[3].split(":")[1]);
                        }
                        break;

                    case "file_upload_response":
                        if (status < 300) {
                            System.out.println("You successfully sended file.");
                        } else {
                            System.out.println(decodedData[3].split(":")[1]);
                        }
                        break;

                    case "file_download_response":
                        if (status < 300) {
                            System.out.println("You successfully downloaded file.");

                            byte[] bytes = Base64.getDecoder().decode(decodedData[3].split(":")[1]);

                            File file = new File("output.txt");

                            FileOutputStream outputStream = new FileOutputStream(file);

                            outputStream.write(bytes);
                            outputStream.close();
                        } else {
                            System.out.println(decodedData[3].split(":")[1]);
                        }
                        break;

                }
            }
            System.out.println("Type in anything and press enter to continue.");
            message = reader.readLine();
            clearConsole();
        }

        reader.close();
        writerToApiGateway.close();
        readerFromApiGateway.close();
        socketToApiGateway.close();

    }
}
