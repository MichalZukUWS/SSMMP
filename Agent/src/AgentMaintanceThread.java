import java.io.IOException;
import java.util.ArrayList;

public class AgentMaintanceThread extends Thread {
    private String paramsString;
    // private Process process;

    public AgentMaintanceThread(String paramsString) {
        this.paramsString = paramsString;
        // process = null;
        start();
    }

    // @Override
    // public void interrupt() {
    // process.destroy();
    // // Poczekaj na zakończenie procesu
    // try {
    // process.waitFor();
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // }

    // if (process.isAlive()) {
    // process.destroyForcibly();
    // // Poczekaj na zakończenie procesu
    // try {
    // process.waitFor();
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // }
    // }
    // }

    @Override
    public void run() {
        try {
            ArrayList<String> runProcesArguments = new ArrayList<>();
            runProcesArguments.add("java");
            runProcesArguments.add("-cp");
            String typeOfService = paramsString.split(";")[3].split(":")[1];
            switch (typeOfService) {
                case "BaaS":
                    runProcesArguments.add("C:\\Users\\An4x\\Desktop\\PROGRAMOWANIE\\TS\\boo\\BaaS\\bin\\");
                    break;
                default:
                    runProcesArguments.add("C:\\Users\\An4x\\Desktop\\PROGRAMOWANIE\\TS\\boo\\Service\\bin\\");
                    break;

            }

            runProcesArguments.add("App");
            runProcesArguments.add(paramsString);

            ProcessBuilder builder = new ProcessBuilder(runProcesArguments);
            builder.redirectErrorStream(true);
            builder.inheritIO();
            // process = builder.start();
            builder.start();
            // process.waitFor();
        } catch (IOException e) {
            System.out.println("Agent Exception FROM Service: " + e.getMessage());
            e.printStackTrace();
        }
        // catch (InterruptedException e) {
        // e.printStackTrace();
        // }
    }
}
