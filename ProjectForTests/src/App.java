import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;

import javax.net.SocketFactory;

public class App {
    public static void main(String[] args) {
        try {
            int boo = 0;
            new TestServer();
            Socket s = new Socket("localhost", 15000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));

            while (boo == 0) {
                Thread.sleep(2500);
                if (reader.markSupported()) {
                    reader.mark(1); // oznacz bieżącą pozycję w strumieniu
                    boo = reader.read(); // odczytaj pojedynczy znak
                    if (boo == -1)
                        break;
                    System.out.println("Single character: " + (char) boo);
                    reader.reset(); // resetuj strumień do ostatnio oznaczonej pozycji
                    String line = reader.readLine(); // odczytaj linię
                    System.out.println("Line: " + line);
                    boo = 0;
                } else {
                    System.out.println("Marking not supported");
                }
            }

        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        }

        // Socket socket = new Socket("localhost", 15000);
        // String data = "something:foo\nwith:new\nline:boo";
        // PrintWriter writer = new PrintWriter(socket.getOutputStream());
        // writer.println(data);
        // writer.flush();
        // String newdata = "something:new\nwith:lines\nline:foo";
        // writer.println(newdata);
        // writer.flush();
        // socket.close();
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

        // ArrayList<String> list = new ArrayList<>();
        // list.add("6-12-2023 15:53:00");
        // list.add("6-12-2023 15:54:05");
        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MM-yyyy
        // HH:mm:ss");

        // // Użyj strumienia do przekształcenia ciągów znaków na LocalDateTime
        // LocalDateTime maxDate = list.stream()
        // .map(dateString -> LocalDateTime.parse(dateString, formatter))
        // // Znajdź maksymalną datę za pomocą komparatora
        // .max(Comparator.naturalOrder())
        // .orElse(null);
        // // LocalDateTime.now().isEqual(maxDate)
        // System.out.println("Największa data w liście: " + maxDate);
        // // System.out.println(list.stream().max(s->s.compareTo()));

        // // list.add("Service1 got data from Api Gateway at: "+ );

        // String tempDateTime = "6-12-2023 15:53:00";
        // String tempDateTime2 = "6-12-2023 15:54:05";

        // LocalDateTime fromList = LocalDateTime.parse(tempDateTime, formatter);
        // LocalDateTime actual = LocalDateTime.parse(tempDateTime2, formatter);

        // Duration duration = Duration.between(fromList, actual);
        // // Math.abs(Duration.between(fromList, actual).getSeconds()) > 60;
        // long secondsDifference = Math.abs(duration.getSeconds());

        // // Sprawdź, czy różnica wynosi mniej niż 60 sekund
        // if (secondsDifference < 60) {
        // System.out.println("Różnica między datami wynosi mniej niż 60 sekund.");
        // } else {
        // System.out.println("Różnica między datami wynosi więcej niż 60 sekund.");
        // }

        // System.out.println(tempDateTime.compareTo(tempDateTime2));
        // // if(fromList.getDayOfMonth() == actual.getDayOfMonth() &&
        // fromList.getMonth()
        // // == actual.getMonth() && fromList.getYear() == actual.getYear() &&
        // fromList)
        // // localDateTime.
    }
}
