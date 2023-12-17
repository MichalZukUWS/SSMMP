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
    // try {
    // process.waitFor();
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // }

    // if (process.isAlive()) {
    // process.destroyForcibly();
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
                    runProcesArguments.add("..\\BaaS\\bin\\;..\\mysql-connector-j-8.2.0\\mysql-connector-j-8.2.0.jar");
                    break;
                default:
                    runProcesArguments.add("..\\Service\\bin\\");
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
