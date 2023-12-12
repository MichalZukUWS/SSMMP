import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class AgentToManagerConnectionThread extends Thread {
  private Socket socketFromAgent;
  private PrintWriter writerToAgent;
  private BufferedReader readerFromAgent;
  private Service1Connections service1Connections;
  private Service2Connections service2Connections;
  private BaaSConnections baaSConnections;
  private String dataToSend;
  private LinkedList<String> requests;
  private int portForService;
  private int agentPort;
  private int serviceInstance;
  private ArrayList<RequestForStartService> requestsForStartServiceList;

  public AgentToManagerConnectionThread(Socket socketToAgent, int portForService, LinkedList<String> requests)
      throws IOException {
    this.socketFromAgent = socketToAgent;
    writerToAgent = new PrintWriter(this.socketFromAgent.getOutputStream());
    readerFromAgent = new BufferedReader(new InputStreamReader(this.socketFromAgent.getInputStream()));
    service1Connections = new Service1Connections();
    service2Connections = new Service2Connections();
    baaSConnections = new BaaSConnections();
    dataToSend = null;
    agentPort = 0;
    serviceInstance = 0;
    this.portForService = portForService;
    this.requests = requests;
    requestsForStartServiceList = new ArrayList<>();
    start();
  }

  // function using for getting current date and time
  private String getDateTimeNowString() {
    return LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("d-MM-y H:m:s"));
  }

  @Override
  public void run() {
    new ManagerCheckServicesActivityThread(service1Connections, service2Connections, baaSConnections, requests);
    // process for reading agent request/responses
    Thread readerBufferThread = new Thread(() -> {
      while (!isInterrupted()) {
        try {
          synchronized (requests) {
            if (readerFromAgent.ready()) {
              String dataFromAgent = readerFromAgent.readLine();
              if (dataFromAgent != null) {
                System.out.println(
                    "Manager -> I got the data from the Agent, and add to the queue: " + dataFromAgent);
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
                writerToAgent.println(dataToSend);
                writerToAgent.flush();
                System.out.println("Manager -> Accepted the Agent's registration and sent back a reply");
                break;

              // Agent started new process with Service
              case "execution_response":
                // TODO: check status and process if Service didn't start
                System.out.println(
                    "Manager -> Received Service launch and registration data, retrieve data");
                RequestForStartService element = requestsForStartServiceList.stream()
                    .filter(request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))
                    .findFirst().orElse(null);

                switch (element.getTypeOfService()) {
                  case "Service1":
                    service1Connections.addHistoryByPort(element.getPortOfService(),
                        "Service1 started at: " + getDateTimeNowString());
                    break;
                  case "Service2":
                    service2Connections.addPort(element.getPortOfService());
                    break;
                  case "BaaS":
                    baaSConnections.addPort(element.getPortOfService());
                    break;
                  default:
                    System.out.println("Manager -> Unknown Service in requestsForStartService list");
                    break;
                }

                System.out.println(
                    "Manager -> Received data on the launch and registration of the Service, I delete the entry");
                requestsForStartServiceList = requestsForStartServiceList.stream()
                    .filter(request -> request.getMessageID() != Integer.parseInt(decodedData[1].split(":")[1]))
                    .collect(Collectors.toCollection(ArrayList::new));
                break;

              // Service wants to connect to another Service
              case "session_request":
                String typeOfService = decodedData[6].split(":")[1];
                String message_id = decodedData[1];
                // Service1, Service2, and BaaS is used in developing stage for testing
                // switch for process connection to specific Service
                switch (typeOfService) {
                  case "Service1":
                    // Service1 isn't running
                    if (service1Connections.getNumberOfServices() == 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println(
                          "Manager -> Accepted request to connect to Service1, Service1 is not running");
                      serviceInstance++;
                      // getting port for new instance
                      int portForS = getNextPortForService();
                      // adding entry to Service1 history
                      service1Connections.addNewConnection(new Service1History(portForS, serviceInstance));
                      service1Connections.updateLastUsedService(serviceInstance);
                      service1Connections.addHistoryByPort(portForS,
                          "Request for Service1, starting Service1 at: "
                              + getDateTimeNowString());
                      service1Connections.printHistoryByPort(portForS);
                      // list that contains informations about request
                      requestsForStartServiceList.add(new RequestForStartService(
                          Integer.parseInt(decodedData[1].split(":")[1]), portForS, typeOfService));
                      dataToSend = "type:execution_request;" + decodedData[1]
                          + ";agent_newtork_address:localhost_" + agentPort + ";Service_name:"
                          + typeOfService
                          + ";Service_instance:" + serviceInstance
                          + ";socket_configuration:localhost_" + portForS
                          + ";plug_configuration:configuration of plugs";
                      writerToAgent.println(dataToSend);
                      writerToAgent.flush();

                      requests.add(requestFromQueue);
                      requests.notify();
                      // Service1 is starting
                    } else if (service1Connections.getNumberOfServices() != 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      requests.add(requestFromQueue);
                      requests.notify();
                      // Service1 started and is ready to connect
                    } else if (service1Connections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to Service1, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_Service_instance_network_address:localhost_"
                          + getServicePort(typeOfService) + ";dest_socket_port:" + getServicePort(typeOfService);
                      writerToAgent.println(dataToSend.replace("agent_to_Manager", "Manager_to_agent"));
                      writerToAgent.flush();
                    }

                    break;

                  case "Service2":
                    if (service2Connections.getNumberOfServices() == 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println(
                          "Manager -> Accepted request to connect to Service2, Service2 is not running");
                      serviceInstance++;
                      int portForS = getNextPortForService();
                      requestsForStartServiceList.add(new RequestForStartService(
                          Integer.parseInt(decodedData[1].split(":")[1]), portForS, typeOfService));
                      dataToSend = "type:execution_request;" + decodedData[1]
                          + ";agent_newtork_address:localhost_" + agentPort + ";Service_name:"
                          + typeOfService
                          + ";Service_instance:" + serviceInstance
                          + ";socket_configuration:localhost_" + portForS
                          + ";plug_configuration:configuration of plugs";
                      writerToAgent.println(dataToSend);
                      writerToAgent.flush();
                      requests.add(requestFromQueue);
                      requests.notify();
                    } else if (service2Connections.getNumberOfServices() == 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      requests.add(requestFromQueue);
                      requests.notify();
                    } else if (service2Connections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to Service2, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_Service_instance_network_address:localhost_"
                          + getServicePort(typeOfService) + ";dest_socket_port:" + getServicePort(typeOfService);
                      writerToAgent.println(dataToSend.replace("agent_to_Manager", "Manager_to_agent"));
                      writerToAgent.flush();
                    }

                    break;

                  case "BaaS":
                    if (baaSConnections.getNumberOfServices() == 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println(
                          "Manager -> Accepted request to connect to BaaS, BaaS is not running");
                      serviceInstance++;
                      int portForS = getNextPortForService();
                      requestsForStartServiceList.add(new RequestForStartService(
                          Integer.parseInt(decodedData[1].split(":")[1]), portForS, typeOfService));
                      dataToSend = "type:execution_request;" + decodedData[1]
                          + ";agent_newtork_address:localhost_" + agentPort + ";Service_name:"
                          + typeOfService
                          + ";Service_instance:" + serviceInstance
                          + ";socket_configuration:localhost_" + portForS
                          + ";plug_configuration:configuration of plugs";
                      writerToAgent.println(dataToSend);
                      writerToAgent.flush();
                      requests.add(requestFromQueue);
                      requests.notify();
                    } else if (baaSConnections.getNumberOfServices() == 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      requests.add(requestFromQueue);
                      requests.notify();
                    } else if (baaSConnections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to BaaS, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_Service_instance_network_address:localhost_"
                          + getServicePort(typeOfService) + ";dest_socket_port:" + getServicePort(typeOfService);
                      writerToAgent.println(dataToSend.replace("agent_to_Manager", "Manager_to_agent"));
                      writerToAgent.flush();
                    }
                    break;
                }
                break;

              // request for registering Services that are running from start of application
              // and aren't starting via Agent
              case "connection_request":
                System.out.println("Manager -> Accepted Api Gateway Connection");
                break;

              // request for information about estabilishing connection Services
              case "session_ack":
                System.out.println("Manager -> Accepted data on the successful connection of Services.");
                // source port is Service1
                if (service1Connections.isServiceWithPort(Integer.parseInt(decodedData[4].split(":")[1]))) {
                  // BaaS is the one possible case of connection
                  service1Connections.addHistoryByPort(Integer.parseInt(decodedData[4].split(":")[1]),
                      "Service1 connected to BaaS at: " + getDateTimeNowString());
                  service1Connections.printHistoryByPort(Integer.parseInt(decodedData[4].split(":")[1]));
                }
                // source port is Service2
                else if (service2Connections.getPorts().stream()
                    .anyMatch(s -> s == Integer.parseInt(decodedData[4].split(":")[1]))) {
                  // BaaS is the one possible case of connection
                }
                // source port is Api Gateway
                else {
                  // destination port is Service1
                  if (service1Connections.isServiceWithPort(Integer.parseInt(decodedData[5].split(":")[1]))) {
                    service1Connections.addHistoryByPort(Integer.parseInt(decodedData[5].split(":")[1]),
                        "Service1 accepted a connection from Api Gateway at: " + getDateTimeNowString());
                    service1Connections.printHistoryByPort(Integer.parseInt(decodedData[5].split(":")[1]));
                  }
                  // destination port is Service2
                  else if (service2Connections.getPorts().stream()
                      .anyMatch(d -> d == Integer.parseInt(decodedData[5].split(":")[1]))) {

                  }
                }
                break;

              // source Service closed connetion with another Service
              case "source_Service_session_close_info":
                // source_Service_name:A
                switch (decodedData[3].split(":")[1]) {
                  case "Api Gateway":
                    // dest_Service_name:B
                    switch (decodedData[8].split(":")[1]) {
                      // 1. Api Gateway -> Service1
                      case "Service1":
                        service1Connections.addHistoryByPort(Integer.parseInt(decodedData[11].split(":")[1]),
                            "Api Gateway send message that closed connection with Service1 at: "
                                + getDateTimeNowString());
                        service1Connections.printHistoryByPort(Integer.parseInt(decodedData[11].split(":")[1]));
                        break;
                      // 2. Api Gateway -> Service2
                      case "Service2":
                        break;

                      default:
                        System.out.println(
                            "Manager -> In source Service session close info is unknown destination serivce name");
                        break;
                    }
                    break;
                  case "Service1":
                    service1Connections.addHistoryByPort(Integer.parseInt(decodedData[6].split(":")[1]),
                        "Service1 send message that closed connection with BaaS at: " + getDateTimeNowString());
                    service1Connections.printHistoryByPort(Integer.parseInt(decodedData[6].split(":")[1]));
                    // 3. Service1 -> BaaS
                    break;

                  case "Service2":
                    // 4. Service2 -> BaaS
                    break;

                  default:
                    System.out.println("Manager -> Unknown source Service session close info");
                    break;
                }
                break;

              // destination Service closed connetion with another Service
              case "dest_Service_session_close_info":
                // dest_Service_name:B
                switch (decodedData[6].split(":")[1]) {
                  // 1. Service1 <- Api Gateway
                  case "Service1":
                    service1Connections.addHistoryByPort(Integer.parseInt(decodedData[10].split(":")[1]),
                        "Service1 send message that closed connection with Api Gateway at: " + getDateTimeNowString());
                    break;
                  // 2. Service2 <- Api Gateway
                  case "Service2":
                    break;
                  case "BaaS":
                    // 3. BaaS <- Service1

                    // 4. BaaS <- Service2
                    break;
                  default:
                    System.out
                        .println("Manager -> In dest Service session close info is unknown destination serivce name");
                    break;
                }
                break;

              // Service isn't working correctly and needs to be closed
              case "health_control_response":
                // TODO:
                break;
              case "process_data":
                switch (decodedData[3].split(":")[1]) {
                  case "Service1":
                    System.out
                        .println(
                            "Manager -> I got data about that Service1 is processing data, updating lastUsedDateTime");
                    int serviceI = Integer.parseInt(decodedData[4].split(":")[1]);
                    service1Connections.updateLastUsedService(serviceI);
                    service1Connections.addHistoryByServiceInstance(serviceI,
                        "Service1 processed data at: " + getDateTimeNowString());
                    service1Connections.printHistoryByServiceInstance(serviceI);
                    break;
                  case "Service2":
                    break;
                  case "BaaS":
                    break;
                  default:
                    System.out.println(
                        "Manager -> Request for data processing comes from an unknown service: " + requestFromQueue);
                    break;
                }
                break;
              case "graceful_shutdown_request":
                writerToAgent.println(requestFromQueue);
                writerToAgent.flush();
                break;
              case "graceful_shutdown_response":
                // TODO: remove here from service1History not when request is sended
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
    switch (typeOfService) {
      case "Service1":
        return service1Connections.getPort();

      // TODO: change to latest used Service/least used Service
      case "Service2":
        return service2Connections.getPorts()
            .get(0);

      case "BaaS":
        return baaSConnections.getPorts()
            .get(0);
      default:
        return -1;
    }
  }

  // function that's return next free port for new Service process
  private int getNextPortForService() {
    if (service1Connections.getNumberOfServices() == 0 && service2Connections.getNumberOfServices() == 0
        && baaSConnections.getNumberOfServices() == 0) {
      int returnedPort = portForService;
      portForService++;
      return returnedPort;
    } else {
      int maxPort = -1;
      int[] tabOfPorts = new int[4];
      if (service1Connections.getNumberOfServices() != 0)
        tabOfPorts[0] = service1Connections.getMaxPort();
      else
        tabOfPorts[0] = -1;
      if (service2Connections.getNumberOfServices() != 0)
        tabOfPorts[1] = service2Connections.getPorts().get(service2Connections.getNumberOfServices() - 1);
      else
        tabOfPorts[1] = -1;
      if (baaSConnections.getNumberOfServices() != 0)
        tabOfPorts[2] = baaSConnections.getPorts().get(baaSConnections.getNumberOfServices() - 1);
      else
        tabOfPorts[2] = -1;
      tabOfPorts[3] = portForService;
      for (int i = 0; i < tabOfPorts.length; i++) {
        if (tabOfPorts[i] > maxPort)
          maxPort = tabOfPorts[i];
      }
      maxPort++;
      return maxPort;
    }
  }
}
