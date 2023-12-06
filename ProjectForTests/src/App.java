import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

public class App {
    public static void main(String[] args) throws UnknownHostException, IOException {
        new TestServer();
        Socket socket = new Socket("localhost", 15000);
        // String data = "something:foo\nwith:new\nline:boo";
        // PrintWriter writer = new PrintWriter(socket.getOutputStream());
        // writer.println(data);
        // writer.flush();
        // String newdata = "something:new\nwith:lines\nline:foo";
        // writer.println(newdata);
        // writer.flush();
        socket.close();
        // try {
        // // ArrayList<String> arguments = new ArrayList<>(3);
        // // arguments.add("cmd.exe");
        // // arguments.add("/c");
        // // arguments.add("dir");
        // // ProcessBuilder builder = new ProcessBuilder(arguments);
        // // builder.inheritIO();
        // // Process process = builder.start();
        // // int exitCode = process.waitFor();
        // // System.out.println("Proces zakończony z kodem wyjścia: " + exitCode);
        // Socket socket = new Socket("localhost", 33010);
        // PrintWriter writer = new PrintWriter(socket.getOutputStream());
        // writer.write("Type:Service1");
        // socket.close();
        // socket = new Socket("localhost", 33010);
        // writer = new PrintWriter(socket.getOutputStream());
        // writer.write("Type:Service2");
        // socket.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }

        // ArrayList<Integer> list = new ArrayList<>();
        // int count = 0;
        // list.add(10);
        // count++;
        // list.add(11);
        // count++;
        // list.add(12);
        // count++;

        // System.out.println("By size -1: " + list.get(list.size() - 1));
        // System.out.println("By count - 1: " + list.get(count - 1));
        // LinkedList<String> list = new LinkedList<>();
        // list.addLast("10");
        // list.addLast("11");
        // list.addLast("12");
        // list.add("10");
        // list.add("11");
        // list.add("12");
        // list.push("10");
        // list.push("11");
        // list.push("12");
        // System.out.println(list.poll());
        // System.out.println(list.poll());
        // System.out.println(list.poll());
        // for (String iterable : list) {
        // System.out.println(iterable);
        // }
        // System.out.println(list.pop());
        // System.out.println(list.pop());
        // System.out.println(list.pop());

        // Date date = new Date();
        // System.out.println("toString: " + date);
        // System.out.println("getTime: " + date.getTime());
        // LocalDateTime localDateTime = LocalDateTime.now();
        // System.out.println("localDateTime.now(): " + LocalDateTime.now());
        // System.out
        // .println(
        // "localDateTime.format(): "
        // + LocalDateTime.now().format(DateTimeFormatter.ofPattern("d-M-y H:m:s")));
        // System.out.println("localDateTime.getHour(): " + localDateTime.getHour());
    }
}
