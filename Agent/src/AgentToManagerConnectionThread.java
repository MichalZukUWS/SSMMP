import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class AgentToManagerConnectionThread extends Thread {

  private Socket socketToManager;
  private PrintWriter writerToManager;
  private BufferedReader readerFromManager;
  private LinkedList<ServiceToAgentMessageWithPort> requests;
  private HashMap<Integer, AgentMaintanceThread> threadsWithProcess;
  private HashMap<Integer, AgentToServiceConnectionThread> sessionRequestMap;
  private int myPort;
  private ArrayList<ServiceConnectionWithAgentEntry> serivcePorts;

  public AgentToManagerConnectionThread(HashMap<Integer, AgentToServiceConnectionThread> servicePorts,
      LinkedList<ServiceToAgentMessageWithPort> requests, int myPort,
      ArrayList<ServiceConnectionWithAgentEntry> serivcePorts)
      throws UnknownHostException, IOException {
    socketToManager = new Socket("localhost", 34010);
    writerToManager = new PrintWriter(socketToManager.getOutputStream());
    readerFromManager = new BufferedReader(new InputStreamReader(socketToManager.getInputStream()));
    this.threadsWithProcess = new HashMap<>();
    this.requests = requests;
    sessionRequestMap = new HashMap<>();
    this.myPort = myPort;
    this.serivcePorts = serivcePorts;
    start();
  }

  public void addDataFromService(String dataToManager,
      int message_id,
      AgentToServiceConnectionThread serviceToAgentConnectionThread) {
    synchronized (requests) {
      requests.add(new ServiceToAgentMessageWithPort(dataToManager,
          message_id,
          serviceToAgentConnectionThread));
      requests.notify();
    }
  }

  @Override
  public void run() {

    String initialDataToSend = "type:initiation_request;message_id:10;agent_network_address:localhost_" + myPort
        + ";service_repository:BaaS,Chat,File,Login,Post,Register";
    writerToManager.println(initialDataToSend);
    writerToManager.flush();
    System.out.println("-- Agent -> -- Registers to manager: " + initialDataToSend);
    try {
      String initialResponse = readerFromManager.readLine();
      System.out.println("-- Agent -> -- After registering, got a response from Manager: " + initialResponse);
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
            System.out.println("-- Agent -> -- Received data from Manager: " + data);
            int sourcePort = -1;
            String[] decodedData = data.split(";");
            String typeOfRequest = decodedData[0].split(":")[1];
            switch (typeOfRequest) {
              case "execution_request":
                int port = Integer.parseInt(decodedData[5].split(":")[1].split("_")[1]);
                System.out.println("-- Agent -> -- Processing the service launch");
                // 1. Starting a new thread; launching process.
                synchronized (serivcePorts) {
                  serivcePorts.add(
                      new ServiceConnectionWithAgentEntry(port, Integer.parseInt(decodedData[4].split(":")[1]),
                          Integer.parseInt(decodedData[1].split(":")[1]), decodedData[3].split(":")[1], null));
                  serivcePorts.notify();
                }
                new AgentMaintanceThread(data);
                // 2a. Waiting for service's response.
                break;

              case "session_response":
                int messageID = Integer.parseInt(decodedData[1].split(":")[1]);
                synchronized (sessionRequestMap) {
                  AgentToServiceConnectionThread sendResponseThead = sessionRequestMap.get(messageID);
                  System.out.println("-- Agent -> -- Retrieves thread from map with messageID: " +
                      messageID);
                  sessionRequestMap.remove(messageID);
                  sendResponseThead.addRequestToService(data.replace("Manager_to_agent", "agent_to_service"));
                  sessionRequestMap.notify();
                }

                break;
              case "source_service_session_close_request":
                // 1. Sending to the ServiceA (ServiceToAgentConnectionThread).
                synchronized (serivcePorts) {

                  serivcePorts.stream()
                      .filter(s -> s.getPort() == Integer.parseInt(data.split(";")[4].split(":")[1].split("_")[1]))
                      .findFirst()
                      .ifPresentOrElse(
                          e -> {
                            e.getServiceToAgentConnectionThread()
                                .addRequestToService(data.replace("Manager_to_agent", "agent_to_source_Service"));
                          },
                          () -> {
                            System.out.println(
                                "-- Agent -> -- Received request to close Service to another Service connection which isn't connected to him.");
                          });
                  serivcePorts.notify();
                }
                // 2a. Waiting for service's response.
                break;

              case "graceful_shutdown_request":
                // 1. Send to Service A(ServiceToAgentConnectionThread).
                synchronized (serivcePorts) {
                  serivcePorts.stream()
                      .filter(s -> s.getServiceInstance() == Integer.parseInt(data.split(";")[4].split(":")[1]))
                      .findFirst()
                      .ifPresentOrElse(
                          entry -> {
                            System.out.println("-- Agent -> -- Set message_id in graceful_shutdown_request: " +
                                Integer.parseInt(data.split(";")[1].split(":")[1]));
                            entry.setMessage_id(Integer.parseInt(data.split(";")[1].split(":")[1]));
                            entry.getServiceToAgentConnectionThread()
                                .addRequestToService(data.replace("Manager_to_agent",
                                    "agent_to_Service_instance"));
                          },
                          () -> {
                            System.out.println(
                                "-- Agent -> -- Received request to soft shut down Service which isn't connected to him.");
                          });
                  serivcePorts.notify();
                }
                // 2a. Waiting for service's response.
                break;

              case "hard_shutdown_request":
                // sourcePort = Integer.parseInt(data.split(";")[4].split(":")[1]);
                synchronized (serivcePorts) {
                  serivcePorts.stream()
                      .filter(s -> s.getPort() == Integer.parseInt(data.split(";")[4].split(":")[1]))
                      .findFirst()
                      .ifPresentOrElse(
                          entry -> entry.getServiceToAgentConnectionThread().interrupt(),
                          () -> System.out.println(
                              "-- Agent -> -- Received request to hard shut down service which isn't connected to him."));
                  serivcePorts.notify();
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
              AgentToServiceConnectionThread thread = dataObject.getServiceToAgentConnectionThread();

              String[] decodedData = dataString.split(";");
              String typeOfRequest = decodedData[0].split(":")[1];
              switch (typeOfRequest) {
                // --------------------------Responses to the manager--------------------------

                // when starting a new process, the process will connect to the Agent(will be
                // start a new ServiceToAgentConnectionThread), which at startup will add a
                // response, which needs to be sent to the manager
                case "execution_response":
                  // 2b. Send a message back to the manager.
                  System.out.println("-- Agent -> -- Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;

                case "source_service_session_close_response":
                  // 2b. Send back to the manager the response to close the connection.
                  System.out.println("-- Agent -> -- Sends data to Manager: " + dataObject.getMessage());
                  writerToManager.println(dataObject.getMessage());
                  writerToManager.flush();
                  break;

                case "graceful_shutdown_response":
                  // 2b. Send back a response to the manager to close the service.
                  System.out.println("-- Agent -> -- Sends data to Manager: " + dataObject.getMessage());
                  writerToManager
                      .println(dataObject.getMessage().replace("Service_instance_to_agent", "agent_to_Manager"));
                  writerToManager.flush();
                  break;

                // -----------------------Requests from the service-----------------------
                case "session_request":
                  synchronized (sessionRequestMap) {
                    sessionRequestMap.put(messageID,
                        thread);
                    sessionRequestMap.notify();
                  }
                  dataString = dataString.replace("service_to_agent", "agent_to_Manager");
                  System.out.println("-- Agent -> -- Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;
                case "source_service_session_close_info":
                  dataString = dataString.replace("service_to_agent", "agent_to_Manager");
                  System.out.println("-- Agent -> -- Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;

                case "dest_service_session_close_info":
                  dataString = dataString.replace("service_to_agent", "agent_to_Manager");
                  System.out.println("-- Agent -> -- Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;

                case "session_ack":
                  dataString = dataObject.getMessage().replace("service_to_agent",
                      "agent_to_Manager");
                  System.out.println("-- Agent -> -- Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;

                case "connection_request":
                  writerToManager.println(dataObject.getMessage());
                  System.out.println("-- Agent -> -- Sends data to Manager: " + dataObject.getMessage());
                  writerToManager.flush();
                  break;
                case "health_control_response":
                  if (Integer.parseInt(decodedData[5].split(":")[1]) >= 300) {
                    dataString = dataObject.getMessage().replace("service_to_agent", "agent_to_Manager");
                    System.out.println("-- Agent -> -- Sends data to Manager: " + dataString);
                    writerToManager.println(dataString);
                    writerToManager.flush();
                  }
                  break;
                case "process_data":
                  dataString = dataObject.getMessage().replace("Service_to_agent", "agent_to_Manager");
                  System.out.println("-- Agent -> -- Sends data to Manager: " + dataString);
                  writerToManager.println(dataString);
                  writerToManager.flush();
                  break;
                default:
                  System.out.println("-- Agent -> -- Unknown request from Service: " + dataObject.getMessage());
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
    System.out.println("-- Agent -> -- Starts threads for receiving data");

  }
}
