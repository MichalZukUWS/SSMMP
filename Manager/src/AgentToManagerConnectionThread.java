import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class AgentToManagerConnectionThread extends Thread {
  private Socket socketFromAgent;
  private PrintWriter writerToAgent;
  private BufferedReader readerFromAgent;
  private String dataToSend;
  private LinkedList<String> requests;
  private int portForService;
  private int agentPort;
  private int serviceInstance;
  private ArrayList<RequestForStartService> requestsForStartServiceList;
  private ServicesConnections connections;
  private int plugPort;
  private int sourcePort;
  private int destPort;
  private HashMap<Integer, Integer> plugsToClose;
  private ArrayList<Integer> serviceInstancesToClose;

  public AgentToManagerConnectionThread(Socket socketToAgent, int portForService, LinkedList<String> requests,
      ServicesConnections connections)
      throws IOException {
    this.socketFromAgent = socketToAgent;
    writerToAgent = new PrintWriter(this.socketFromAgent.getOutputStream());
    readerFromAgent = new BufferedReader(new InputStreamReader(this.socketFromAgent.getInputStream()));
    dataToSend = null;
    agentPort = 0;
    serviceInstance = 9;
    this.portForService = portForService;
    this.requests = requests;
    requestsForStartServiceList = new ArrayList<>();
    serviceInstancesToClose = new ArrayList<>();
    this.connections = connections;
    plugPort = 10050;
    plugsToClose = new HashMap<>();
    start();
  }

  public void addRequestToAgent(String request) {
    synchronized (requests) {
      requests.add(request);
      requests.notify();
    }
  }

  // function using for getting current date and time
  private String getDateTimeNowString() {
    return LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("d-MM-y H:m:s"));
  }

  @Override
  public void run() {
    new ManagerCheckServicesActivityThread(connections, plugsToClose, serviceInstancesToClose, this);
    // process for reading agent request/responses
    Thread readerBufferThread = new Thread(() -> {
      while (!isInterrupted()) {
        try {
          synchronized (requests) {
            if (readerFromAgent.ready()) {
              String dataFromAgent = readerFromAgent.readLine();
              if (dataFromAgent != null) {
                System.out.println(
                    "\nManager -> I got the data from the Agent, and add to the queue: " +
                        dataFromAgent);
                requests.add(dataFromAgent);
                requests.notify();
              }
            }
          }
        } catch (IOException e) {
          System.out.println("Manager Exception: " + e.getMessage());
          e.printStackTrace();
        }

      }

    });
    // Thread for processing request/responses
    Thread processQueue = new Thread(() -> {
      while (!isInterrupted()) {
        synchronized (requests) {
          if (requests.size() != 0) {
            String requestFromQueue = requests.poll();
            String[] decodedData = requestFromQueue.split(";");

            switch (decodedData[0].split(":")[1]) {
              // Agent is registering to Manager
              case "initiation_request":
                dataToSend = "type:initiation_response;" + decodedData[1]
                    + ";status:200";
                agentPort = Integer.parseInt(decodedData[2].split(":")[1].split("_")[1]);
                portForService++;
                synchronized (connections) {
                  connections.initializeConnection(requestFromQueue.split(";")[3].split(":")[1].split(","));
                  connections.notify();
                }
                writerToAgent.println(dataToSend);
                writerToAgent.flush();
                System.out.println("Manager -> Accepted the Agent's registration and sent back a reply");
                break;

              // Agent started new process with Service
              case "execution_response":
                // TODO: check status and process if Service didn't start
                System.out.println(
                    "Manager -> Received Service launch and registration data, retrieve data");
                requestsForStartServiceList.stream()
                    .filter(request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))
                    .findFirst().ifPresentOrElse(
                        s -> {
                          connections.addHistoryByPort(s.getPortOfService(),
                              connections.getTypeOfServiceByPort(s.getPortOfService()) + " started at: "
                                  + getDateTimeNowString());
                          connections.setRunningByPort(s.getPortOfService(), true);
                          System.out.println(
                              "Manager -> Received data on the launch and registration of the Service, Deleting entry");
                          requestsForStartServiceList
                              .removeIf(
                                  request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]));
                        },
                        () -> {
                          System.out
                              .println("Manager -> Received a service launch message with an unknown message_id ");
                        });
                break;

              // Service wants to connect to another Service
              case "session_request":
                String typeOfService = decodedData[6].split(":")[1];
                String message_id = decodedData[1];
                // Service1, Service2, and BaaS is used in developing stage for testing
                // switch for process connection to specific Service
                if (connections.getNumberOfServices(typeOfService) == 0
                    && !requestsForStartServiceList.stream().anyMatch(
                        request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                  System.out.println(
                      "Manager -> Accepted request to connect to " + typeOfService + ", " + typeOfService
                          + " is not running");
                  serviceInstance++;
                  int portForS = getNextPortForService();
                  connections.addNewConnection(typeOfService,
                      new ServiceHistory(portForS, serviceInstance, typeOfService));
                  connections.updateLastUsedService(serviceInstance);
                  connections.addHistoryByPort(portForS,
                      "Request for " + typeOfService + ", starting " + typeOfService + " at: "
                          + getDateTimeNowString());
                  connections.printHistoryByPort(portForS);
                  requestsForStartServiceList.add(new RequestForStartService(
                      Integer.parseInt(decodedData[1].split(":")[1]), portForS, typeOfService));
                  dataToSend = "type:execution_request;" + decodedData[1]
                      + ";agent_newtork_address:localhost_" + agentPort + ";Service_name:"
                      + typeOfService
                      + ";Service_instance:" + serviceInstance
                      + ";socket_configuration:localhost_" + portForS
                      + ";plug_configuration:" + plugPort++;
                  writerToAgent.println(dataToSend);
                  writerToAgent.flush();

                  requests.add(requestFromQueue);
                  requests.notify();
                } else if (connections.getNumberOfServices(typeOfService) != 0
                    && requestsForStartServiceList.stream().anyMatch(
                        request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                  // Service is starting
                  requests.add(requestFromQueue);
                  requests.notify();
                } else if (connections.getNumberOfServices(typeOfService) != 0
                    && !requestsForStartServiceList.stream().anyMatch(
                        request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                  // Service started and is ready to connect
                  System.out
                      .println("Manager -> Accepted request to connect to " + typeOfService + ", sends back data");
                  dataToSend = "type:session_response;" + message_id
                      + ";sub_type:Manager_to_agent;status:200;dest_Service_instance_network_address:localhost_"
                      + getServicePort(typeOfService) + ";dest_socket_port:" + getServicePort(typeOfService);
                  writerToAgent.println(dataToSend);
                  writerToAgent.flush();
                }
                break;

              // request for registering Services that are running from start of application and aren't starting via Agent
              case "connection_request":
                System.out.println("Manager -> Accepted Api Gateway Connection");
                synchronized (connections) {
                  connections.addNewConnection("Api_Gateway",
                      new ServiceHistory(34100, 1, "Api_Gateway"));
                }
                break;

              // request for information about estabilishing connection Services
              case "session_ack":
                System.out.println("Manager -> Accepted data on the successful connection of Services.");
                sourcePort = Integer.parseInt(decodedData[4].split(":")[1]);
                destPort = Integer.parseInt(decodedData[5].split(":")[1]);
                if (connections.isServiceWithPlug(sourcePort) && connections.isServiceWithPort(destPort)) {
                  String sourceServiceType = connections.getTypeOfServiceByPlug(sourcePort);
                  String destServiceType = connections.getTypeOfServiceByPort(destPort);

                  connections.setPlugIsConneted(sourcePort, true);
                  connections.setPlugIsConneted(destPort, true);
                  connections.addHistoryByServiceInstance(Integer.parseInt(decodedData[1].split(":")[1]),
                      sourceServiceType + " connected to " + destServiceType + " at: " + getDateTimeNowString());
                  connections.addHistoryByPort(destPort,
                      destServiceType + " accepted a connection from " + sourceServiceType + " at: "
                          + getDateTimeNowString());
                } else {
                  // TODO: some service isn't in connections list
                }
                break;

              // source Service closed connetion with another Service
              case "source_service_session_close_info":
                destPort = Integer.parseInt(decodedData[11].split(":")[1]);
                sourcePort = Integer.parseInt(decodedData[7].split(":")[1]);

                if (connections.isServiceWithPlug(sourcePort) && connections.isServiceWithPort(destPort)) {
                  String sourceServiceType = connections.getTypeOfServiceByPlug(sourcePort);
                  String destServiceType = connections.getTypeOfServiceByPort(destPort);

                  connections.addHistoryByPort(sourcePort, sourceServiceType + " closed connection with "
                      + destServiceType + " at: " + getDateTimeNowString());
                  connections.setRunningByPort(sourcePort, false);
                } else {
                  // TODO: some service isn't in connections list
                }
                break;

              // destination Service closed connetion with another Service
              case "dest_service_session_close_info":
                destPort = Integer.parseInt(decodedData[10].split(":")[1]);
                sourcePort = Integer.parseInt(decodedData[5].split(":")[1]);

                if (connections.isServiceWithPlug(sourcePort) && connections.isServiceWithPort(destPort)) {
                  String sourceServiceType = connections.getTypeOfServiceByPlug(sourcePort);
                  String destServiceType = connections.getTypeOfServiceByPort(destPort);

                  connections.addHistoryByPort(destPort,
                      destServiceType + " connection with " + sourceServiceType + " at: " + getDateTimeNowString());
                  connections.setRunningByPort(destPort, false);

                } else {
                  // TODO: some service isn't in connections list
                }
                break;

              // Service isn't working correctly and needs to be closed
              case "health_control_response":
                // TODO: send request to hard close service
                break;
              case "process_data":
                int serviceI = Integer.parseInt(decodedData[3].split(":")[1]);
                int servicePlug = Integer.parseInt(decodedData[4].split(":")[1]);
                if (connections.isServiceWithNumberOfInstance(serviceI)) {
                  String typeOfServicee = connections.getTypeOfServiceByNumberOfInstance(serviceI);
                  connections.updateLastUsedService(serviceI);
                  connections.updateLastUsedPlug(servicePlug);
                  connections.addHistoryByServiceInstance(serviceI,
                      typeOfServicee + " processed data at: " + getDateTimeNowString());
                  connections.printHistoryByServiceInstance(serviceI);
                } else {
                  // TODO: service isn't in connections list
                }
                break;
              case "graceful_shutdown_request":
                writerToAgent.println(requestFromQueue);
                writerToAgent.flush();
                break;
              case "graceful_shutdown_response":
                synchronized (serviceInstancesToClose) {
                  if (serviceInstancesToClose.contains(Integer.parseInt(decodedData[1].split(":")[1]))) {
                    serviceInstancesToClose
                        .remove(serviceInstancesToClose.indexOf(Integer.parseInt(decodedData[1].split(":")[1])));
                    connections.setRunningByPort(Integer.parseInt(decodedData[1].split(":")[1]), false);
                  } else {
                    // TODO: service which shouldn't shutdown
                  }
                  serviceInstancesToClose.notify();
                }
                break;
              case "source_service_session_close_request":
                writerToAgent.println(requestFromQueue);
                writerToAgent.flush();
                break;
              case "source_service_session_close_response":
                synchronized (plugsToClose) {
                  if (plugsToClose.keySet().contains(Integer.parseInt(decodedData[1].split(":")[1]))) {
                    connections.setPlugIsConneted(plugsToClose.get(Integer.parseInt(decodedData[1].split(":")[1])),
                        false);
                    plugsToClose.remove(Integer.parseInt(decodedData[1].split(":")[1]));
                  } else {
                    System.err
                        .println("Manager -> Got response about closed session from service that shouldn't do that");
                    // TODO: service with this number of instance shoudn't close plug
                  }
                  plugsToClose.notify();
                }
                break;
              default:
                System.out.println("Manager -> Unknown request: " + requestFromQueue);
                break;
            }
            try {
              Thread.sleep(10);
            } catch (InterruptedException e) {
              System.out.println("Manager Exception: " + e.getMessage());
              e.printStackTrace();
            }
          }
        }

      }
    });

    System.out.println("Manager -> Starting a thread for receiving data and processing the queue");
    readerBufferThread.start();
    processQueue.start();
  }

  // function for getting port to Service
  private int getServicePort(String typeOfService) {
    return connections.getPort(typeOfService);
  }

  // function that's return next free port for new Service process
  private int getNextPortForService() {

    if (connections.getNumberOfServices() == 0) {
      int returnedPort = portForService;
      portForService++;
      return returnedPort;
    }
    int returnedPort = portForService;
    if (connections.getMaxPort() > portForService) {
      returnedPort = connections.getMaxPort();
      returnedPort++;
    } else {
      returnedPort++;
      portForService++;
    }
    return returnedPort;
  }
}
