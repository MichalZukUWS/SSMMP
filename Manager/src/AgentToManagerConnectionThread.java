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

  public AgentToManagerConnectionThread(Socket socketToAgent, int portForService) throws IOException {
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
    requests = new LinkedList<>();
    requestsForStartServiceList = new ArrayList<>();
    start();
  }

  private String getDateTimeNowString() {
    return LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("d-M-y H:m:s"));
  }

  @Override
  public void run() {

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
    Thread processQueue = new Thread(() -> {
      while (!isInterrupted()) {
        synchronized (requests) {
          if (requests.size() != 0) {

            String requestFromQueue = requests.poll();
            String[] decodedData = requestFromQueue.split(";");

            switch (decodedData[0].split(":")[1]) {
              case "initiation_request":
                dataToSend = "type:initiation_response;" + decodedData[1]
                    + ";status:200";
                agentPort = Integer.parseInt(decodedData[2].split(":")[1].split("_")[1]);
                portForService++;
                writerToAgent.println(dataToSend);
                writerToAgent.flush();
                System.out.println("Manager -> Accepted the Agent's registration and sent back a reply");
                break;

              case "execution_response":
                System.out.println(
                    "Manager -> Received service launch and registration data, retrieve data");
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
                    System.out.println("Manager -> Unknown service in requestsForStartService list");
                    break;
                }

                System.out.println(
                    "Manager -> Received data on the launch and registration of the service, I delete the entry");
                requestsForStartServiceList = requestsForStartServiceList.stream()
                    .filter(request -> request.getMessageID() != Integer.parseInt(decodedData[1].split(":")[1]))
                    .collect(Collectors.toCollection(ArrayList::new));
                break;

              case "session_request":
                String typeOfService = decodedData[6].split(":")[1];
                String message_id = decodedData[1];
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
                      int portForService = getNextPortForService();
                      // adding entry to Service1 history
                      service1Connections.addNewConnection(new Service1History(portForService));
                      service1Connections.addHistoryByPort(portForService,
                          "Request for Service1, starting Service1 at: "
                              + getDateTimeNowString());
                      service1Connections.printHistoryByPort(portForService);
                      // list that contains informations about request
                      requestsForStartServiceList.add(new RequestForStartService(
                          Integer.parseInt(decodedData[1].split(":")[1]), portForService, typeOfService));
                      dataToSend = "type:execution_request;" + decodedData[1]
                          + ";agent_newtork_address:localhost_" + agentPort + ";service_name:"
                          + typeOfService
                          + ";service_instance:" + serviceInstance
                          + ";socket_configuration:localhost_" + portForService
                          + ";plug_configuration:configuration of plugs";
                      writerToAgent.println(dataToSend);
                      writerToAgent.flush();

                      requests.add(requestFromQueue);
                      requests.notify();
                      // Service1 is starting
                    } else if (service1Connections.getNumberOfServices() != 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      // System.out.println(
                      // "Manager -> Accepted a request to connect to Service1, Service1 is starting
                      // up");
                      requests.add(requestFromQueue);
                      requests.notify();
                      // Service1 started and is ready to connect
                    } else if (service1Connections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to Service1, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_service_instance_network_address:localhost_"
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
                      int portForService = getNextPortForService();
                      requestsForStartServiceList.add(new RequestForStartService(
                          Integer.parseInt(decodedData[1].split(":")[1]), portForService, typeOfService));
                      dataToSend = "type:execution_request;" + decodedData[1]
                          + ";agent_newtork_address:localhost_" + agentPort + ";service_name:"
                          + typeOfService
                          + ";service_instance:" + serviceInstance
                          + ";socket_configuration:localhost_" + portForService
                          + ";plug_configuration:configuration of plugs";
                      writerToAgent.println(dataToSend);
                      writerToAgent.flush();
                      requests.add(requestFromQueue);
                      requests.notify();
                    } else if (service2Connections.getNumberOfServices() == 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      // System.out.println(
                      // "Manager -> Accepted a request to connect to Service2, Service2 is starting
                      // up");
                      requests.add(requestFromQueue);
                      requests.notify();
                    } else if (service2Connections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to Service2, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_service_instance_network_address:localhost_"
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
                      int portForService = getNextPortForService();
                      requestsForStartServiceList.add(new RequestForStartService(
                          Integer.parseInt(decodedData[1].split(":")[1]), portForService, typeOfService));
                      dataToSend = "type:execution_request;" + decodedData[1]
                          + ";agent_newtork_address:localhost_" + agentPort + ";service_name:"
                          + typeOfService
                          + ";service_instance:" + serviceInstance
                          + ";socket_configuration:localhost_" + portForService
                          + ";plug_configuration:configuration of plugs";
                      writerToAgent.println(dataToSend);
                      writerToAgent.flush();
                      requests.add(requestFromQueue);
                      requests.notify();
                    } else if (baaSConnections.getNumberOfServices() == 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      // System.out.println(
                      // "Manager -> Accepted a request to connect to BaaS, BaaS is starting up");
                      requests.add(requestFromQueue);
                      requests.notify();
                    } else if (baaSConnections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to BaaS, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_service_instance_network_address:localhost_"
                          + getServicePort(typeOfService) + ";dest_socket_port:" + getServicePort(typeOfService);
                      writerToAgent.println(dataToSend.replace("agent_to_Manager", "Manager_to_agent"));
                      writerToAgent.flush();
                    }
                    break;
                }
                break;

              case "connection_request":
                System.out.println("Manager -> Accepted Api Gateway Connection");
                break;

              case "session_ack":
                System.out.println("Manager -> Accepted data on the successful connection of services.");
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

              case "source_service_session_close_info":
                // source_service_name:A
                switch (decodedData[3].split(":")[1]) {
                  case "Api Gateway":
                    // dest_service_name:B
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
                            "Manager -> In source service session close info is unknown destination serivce name");
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
                    System.out.println("Manager -> Unknown source service session close info");
                    break;
                }
                break;

              case "dest_service_session_close_info":
                // dest_service_name:B
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
                        .println("Manager -> In dest service session close info is unknown destination serivce name");
                    break;
                }
                break;

              case "health_control_response":
                // TODO:
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

    System.out.println("Manager ->  Starting a thread for receiving data and processing the queue");
    readerBufferThread.start();
    processQueue.start();
  }

  private int getServicePort(String typeOfService) {
    switch (typeOfService) {
      // TODO: change to latest used service/least used service
      case "Service1":
        return service1Connections.getPort();

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

  private int getNextPortForService() {
    if (service1Connections.getNumberOfServices() == 0 && service2Connections.getNumberOfServices() == 0
        && baaSConnections.getNumberOfServices() == 0) {
      int returnedPort = portForService;
      portForService++;
      return returnedPort;
    } else {
      int maxPort = -1;
      int[] tabOfPorts = new int[3];
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
      for (int i = 0; i < tabOfPorts.length; i++) {
        if (tabOfPorts[i] > maxPort)
          maxPort = tabOfPorts[i];
      }
      maxPort++;
      return maxPort;
    }
  }
}
