import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;

public class AgentToManagerConnectionThread extends Thread {

  private Socket socketToManager;
  private PrintWriter writerToManager;
  private BufferedReader readerFromManager;
  private HashMap<Integer, ServiceToAgentConnectionThread> servicePorts;
  private LinkedList<ServiceToAgentMessageWithPort> requests;
  private HashMap<Integer, AgentMaintanceThread> threadsWithProcess;
  private HashMap<Integer, ServiceToAgentConnectionThread> sessionRequestMap;
  private int myPort;

  public AgentToManagerConnectionThread(HashMap<Integer, ServiceToAgentConnectionThread> servicePorts,
      LinkedList<ServiceToAgentMessageWithPort> requests, int myPort)
      throws UnknownHostException, IOException {
    socketToManager = new Socket("localhost", 34010);
    writerToManager = new PrintWriter(socketToManager.getOutputStream());
    readerFromManager = new BufferedReader(new InputStreamReader(socketToManager.getInputStream()));
    this.servicePorts = servicePorts;
    this.threadsWithProcess = new HashMap<>();
    this.requests = requests;
    sessionRequestMap = new HashMap<>();
    this.myPort = myPort;
    start();
  }

  // public void addDataFromService(String dataToManager,
  // int message_id,
  // ServiceToAgentConnectionThread serviceToAgentConnectionThread) {
  // synchronized (requests) {
  // requests.add(new ServiceToAgentMessageWithPort(dataToManager,
  // message_id,
  // serviceToAgentConnectionThread));
  // // requests.notify();
  // }
  // }

  @Override
  public void run() {

    String initialDataToSend = "type:initiation_request;message_id:10;agent_network_address:localhost_" + myPort
        + ";service_repository:Service1,Service2,BaaS";
    writerToManager.println(initialDataToSend);
    writerToManager.flush();
    System.out.println("Agent -> Registers to manager: " + initialDataToSend);
    try {
      String initialResponse = readerFromManager.readLine();
      System.out.println("Agent -> After registering, got a response from Manager: " + initialResponse);
    } catch (IOException e) {
      System.out.println("Agent Exception: " + e.getMessage());
      e.printStackTrace();
    }

    // thread to check the buffer and handle data
    Thread checkBufferThread = new Thread(() -> {
      // check if new data is available in the buffer
      while (!isInterrupted()) {
        try {
          if (readerFromManager.ready()) {
            String data = readerFromManager.readLine();
            System.out.println("Agent -> Received data from Manager: " + data);
            int sourcePort = -1;
            String[] decodedData = data.split(";");
            String typeOfRequest = decodedData[0].split(":")[1];
            switch (typeOfRequest) {
              case "execution_request":
                int port = Integer.parseInt(decodedData[5].split(":")[1].split("_")[1]);
                System.out.println("Agent -> Processing the service launch");
                // 1. Starting a new thread; launching process.
                threadsWithProcess.put(port, new AgentMaintanceThread(data));
                synchronized (servicePorts) {
                  servicePorts.put(port, null);
                  servicePorts.notify();
                }
                // 2a. Waiting for service's response.
                break;

              case "session_response":
                int messageID = Integer.parseInt(decodedData[1].split(":")[1]);
                synchronized (sessionRequestMap) {
                  ServiceToAgentConnectionThread sendResponseThead = sessionRequestMap.get(messageID);
                  System.out.println("Agent -> Retrieves thread from map with messageID: " +
                      messageID);
                  sessionRequestMap.remove(messageID);
                  sessionRequestMap.notify();
                  sendResponseThead
                      .addRequestToService(data.replace("Manager_to_agent", "agent_to_service"));
                }

                break;
              case "source_service_session_close_request":
                sourcePort = Integer
                    .parseInt(initialDataToSend.split(";")[4].split(":")[1].split("_")[1]);
                // 1. Sending to the ServiceA (ServiceToAgentConnectionThread).
                synchronized (servicePorts) {
                  servicePorts.get(sourcePort).addRequestToService(data);
                  servicePorts.notify();
                }
                // 2a. Waiting for service's response.
                break;

              case "graceful_shutdown_request":
                sourcePort = Integer
                    .parseInt(initialDataToSend.split(";")[4].split(":")[1]);
                // 1. Send to Service A(ServiceToAgentConnectionThread).
                synchronized (servicePorts) {
                  servicePorts.get(sourcePort).addRequestToService(data);
                  servicePorts.notify();
                }
                // 2a. Waiting for service's response.
                break;

              case "hard_shutdown_request":
                sourcePort = Integer.parseInt(initialDataToSend.split(";")[4].split(":")[1]);
                // 1. Closing the connection thread.
                synchronized (servicePorts) {
                  servicePorts.get(sourcePort).interrupt();
                  servicePorts.notify();
                }
                // 2. Termination of the thread in which the service process is running.
                threadsWithProcess.get(sourcePort).interrupt();
                // 3. Send the answer back to the manager.
                writerToManager.println(data);
                writerToManager.flush();
                break;

              default:
                break;
            }

          }
        } catch (IOException e) {
          System.out.println("Agent Exception: " + e.getMessage());
          e.printStackTrace();
        }
      }
    });

    // thread to handle LinkedList data
    Thread handleListDataThread = new Thread(() -> {
      while (!isInterrupted()) {
        synchronized (requests) {
          if (requests.size() != 0) {
            ServiceToAgentMessageWithPort dataObject = requests.pollFirst();
            requests.notify();
            if (dataObject != null) {
              String dataString = dataObject.getMessage();
              int messageID = dataObject.getMessage_id();
              ServiceToAgentConnectionThread thread = dataObject.getServiceToAgentConnectionThread();

              String[] decodedData = dataString.split(";");
              String typeOfRequest = decodedData[0].split(":")[1];
              switch (typeOfRequest) {
                // --------------------------Responses to the manager--------------------------

                // when starting a new process, the process will connect to the Agent(will be
                // start a new ServiceToAgentConnectionThread), which at startup will add a
                // response, which needs to be sent to the manager
                case "execution_response":
                  // 2b. Send a message back to the manager.
                  System.out.println("Agent -> Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;

                case "source_service_session_close_response":
                  // 2b. Send back to the manager the response to close the connection.
                  System.out.println("Agent -> Sends data to Manager: " + dataObject.getMessage());
                  writerToManager.println(dataObject.getMessage());
                  writerToManager.flush();
                  break;

                case "graceful_shutdown_response":
                  // 2b. Send back a response to the manager to close the service.
                  System.out.println("Agent -> Sends data to Manager: " + dataObject.getMessage());
                  writerToManager.println(dataObject.getMessage());
                  writerToManager.flush();
                  break;

                // -----------------------Requests from the service-----------------------
                case "session_request":
                  synchronized (sessionRequestMap) {
                    // System.out.println("Agent -> Adding messageID to the map (sessionRequestMap):
                    // "
                    // + messageID);
                    sessionRequestMap.put(messageID,
                        thread);
                    sessionRequestMap.notify();
                  }
                  dataString = dataString.replace("service_to_agent", "agent_to_Manager");
                  System.out.println("Agent -> Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;
                case "source_service_session_close_info":
                  dataString = dataString.replace("service_to_agent", "agent_to_Manager");
                  System.out.println("Agent -> Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;

                case "dest_service_session_close_info":
                  dataString = dataString.replace("service_to_agent", "agent_to_Manager");
                  System.out.println("Agent -> Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;

                case "session_ack":
                  dataString = dataObject.getMessage().replace("service_to_agent",
                      "agent_to_Manager");
                  System.out.println("Agent -> Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;

                case "connection_request":
                  writerToManager.println(dataObject.getMessage());
                  System.out.println("Agent -> Sends data to Manager: " + dataObject.getMessage());
                  writerToManager.flush();
                  break;
                case "health_control_response":
                  if (Integer.parseInt(decodedData[5].split(":")[1]) >= 300) {
                    dataString = dataObject.getMessage().replace("service_to_agent", "agent_to_Manager");
                    System.out.println("Agent -> Sends data to Manager: " + dataString);
                    writerToManager.println(dataString);
                    writerToManager.flush();
                  }
                  break;
                default:
                  break;
              }
            }
            try {
              Thread.sleep(10);
            } catch (InterruptedException e) {
              System.out.println("Agent Exception: " + e.getMessage());
              e.printStackTrace();
            }
          }
        }

      }
    });

    checkBufferThread.start();
    handleListDataThread.start();
    System.out.println("Agent -> Starts threads for receiving data");

  }
}
