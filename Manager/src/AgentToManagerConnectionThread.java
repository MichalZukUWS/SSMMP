import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;

public class AgentToManagerConnectionThread extends Thread {
  private Socket socketFromAgent;
  private PrintWriter writerToAgent;
  private BufferedReader readerFromAgent;
  private BaaSConnections baaSConnections;
  private ChatConnections chatConnections;
  private FileConnections fileConnections;
  private LoginConnections loginConnections;
  private PostsConnections postsConnections;
  private RegisterConnections registerConnections;
  private String dataToSend;
  private LinkedList<String> requests;
  private int portForService;
  private int agentPort;
  private int serviceInstance;
  private ArrayList<RequestForStartService> requestsForStartServiceList;

  // baaSConnections
  // chatConnections
  // fileConnections
  // loginConnections
  // postsConnections
  // registerConnections

  public AgentToManagerConnectionThread(Socket socketToAgent, int portForService, LinkedList<String> requests)
      throws IOException {
    this.socketFromAgent = socketToAgent;
    writerToAgent = new PrintWriter(this.socketFromAgent.getOutputStream());
    readerFromAgent = new BufferedReader(new InputStreamReader(this.socketFromAgent.getInputStream()));
    baaSConnections = new BaaSConnections();
    chatConnections = new ChatConnections();
    fileConnections = new FileConnections();
    loginConnections = new LoginConnections();
    postsConnections = new PostsConnections();
    registerConnections = new RegisterConnections();
    dataToSend = null;
    agentPort = 0;
    serviceInstance = 9;
    this.portForService = portForService;
    this.requests = requests;
    requestsForStartServiceList = new ArrayList<>();
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
    new ManagerCheckServicesActivityThread(baaSConnections, chatConnections, fileConnections, loginConnections,
        postsConnections, registerConnections, this);
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
                writerToAgent.println(dataToSend);
                writerToAgent.flush();
                System.out.println("Manager -> Accepted the Agent's registration and sent back a reply");
                break;

              // Agent started new process with Service
              case "execution_response":
                // TODO: check status and process if Service didn't start
                System.out.println(
                    "Manager -> Received Service launch and registration data, retrieve data");
                Optional<RequestForStartService> element = requestsForStartServiceList.stream()
                    .filter(request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))
                    .findFirst();
                ;
                if (!element.isPresent()) {
                  System.out.println("Manager -> Received a service launch message with an unknown message_id ");
                } else {
                  switch (element.get().getTypeOfService()) {
                    case "BaaS":
                      baaSConnections.addHistoryByPort(element.get().getPortOfService(),
                          "BaaS started at: " + getDateTimeNowString());
                      break;
                    case "Chat":
                      chatConnections.addHistoryByPort(element.get().getPortOfService(),
                          "Chat started at: " + getDateTimeNowString());
                      break;
                    case "File":
                      fileConnections.addHistoryByPort(element.get().getPortOfService(),
                          "File started at: " + getDateTimeNowString());
                      break;
                    case "Login":
                      loginConnections.addHistoryByPort(element.get().getPortOfService(),
                          "Login started at: " + getDateTimeNowString());
                      break;
                    case "Post":
                      postsConnections.addHistoryByPort(element.get().getPortOfService(),
                          "Post started at: " + getDateTimeNowString());
                      break;
                    case "Register":
                      registerConnections.addHistoryByPort(element.get().getPortOfService(),
                          "Register started at: " + getDateTimeNowString());
                      break;
                    default:
                      System.out.println("Manager -> Unknown Service in requestsForStartService list");
                      break;
                  }
                  System.out.println(
                      "Manager -> Received data on the launch and registration of the Service, I delete the entry");
                  requestsForStartServiceList
                      .removeIf(request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]));
                }
                break;

              // Service wants to connect to another Service
              case "session_request":
                String typeOfService = decodedData[6].split(":")[1];
                String message_id = decodedData[1];
                // Service1, Service2, and BaaS is used in developing stage for testing
                // switch for process connection to specific Service
                switch (typeOfService) {
                  case "BaaS":
                    // BaaS isn't running
                    if (baaSConnections.getNumberOfServices() == 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println(
                          "Manager -> Accepted request to connect to BaaS, BaaS is not running");
                      serviceInstance++;
                      // getting port for new instance
                      int portForS = getNextPortForService();
                      // adding entry to BaaS history
                      baaSConnections.addNewConnection(new BaaSHistory(portForS, serviceInstance));
                      baaSConnections.updateLastUsedService(serviceInstance);
                      baaSConnections.addHistoryByPort(portForS,
                          "Request for BaaS, starting BaaS at: "
                              + getDateTimeNowString());
                      baaSConnections.printHistoryByPort(portForS);
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
                      // BaaS is starting
                    } else if (baaSConnections.getNumberOfServices() != 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      requests.add(requestFromQueue);
                      requests.notify();
                      // BaaS started and is ready to connect
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
                  case "Chat":
                    // Chat isn't running
                    if (chatConnections.getNumberOfServices() == 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println(
                          "Manager -> Accepted request to connect to Chat, Chat is not running");
                      serviceInstance++;
                      // getting port for new instance
                      int portForS = getNextPortForService();
                      // adding entry to Chat history
                      chatConnections.addNewConnection(new ChatHistory(portForS, serviceInstance));
                      chatConnections.updateLastUsedService(serviceInstance);
                      chatConnections.addHistoryByPort(portForS,
                          "Request for Chat, starting Chat at: "
                              + getDateTimeNowString());
                      chatConnections.printHistoryByPort(portForS);
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
                      // Chat is starting
                    } else if (chatConnections.getNumberOfServices() != 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      requests.add(requestFromQueue);
                      requests.notify();
                      // Chat started and is ready to connect
                    } else if (chatConnections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to Chat, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_Service_instance_network_address:localhost_"
                          + getServicePort(typeOfService) + ";dest_socket_port:" + getServicePort(typeOfService);
                      writerToAgent.println(dataToSend.replace("agent_to_Manager", "Manager_to_agent"));
                      writerToAgent.flush();
                    }
                    break;
                  case "File":
                    // File isn't running
                    if (fileConnections.getNumberOfServices() == 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println(
                          "Manager -> Accepted request to connect to File, File is not running");
                      serviceInstance++;
                      // getting port for new instance
                      int portForS = getNextPortForService();
                      // adding entry to File history
                      fileConnections.addNewConnection(new FileHistory(portForS, serviceInstance));
                      fileConnections.updateLastUsedService(serviceInstance);
                      fileConnections.addHistoryByPort(portForS,
                          "Request for File, starting File at: "
                              + getDateTimeNowString());
                      fileConnections.printHistoryByPort(portForS);
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
                      // File is starting
                    } else if (fileConnections.getNumberOfServices() != 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      requests.add(requestFromQueue);
                      requests.notify();
                      // File started and is ready to connect
                    } else if (fileConnections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to File, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_Service_instance_network_address:localhost_"
                          + getServicePort(typeOfService) + ";dest_socket_port:" + getServicePort(typeOfService);
                      writerToAgent.println(dataToSend.replace("agent_to_Manager", "Manager_to_agent"));
                      writerToAgent.flush();
                    }
                    break;
                  case "Login":
                    // Login isn't running
                    if (loginConnections.getNumberOfServices() == 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println(
                          "Manager -> Accepted request to connect to Login, Login is not running");
                      serviceInstance++;
                      // getting port for new instance
                      int portForS = getNextPortForService();
                      // adding entry to File history
                      loginConnections.addNewConnection(new LoginHistory(portForS, serviceInstance));
                      loginConnections.updateLastUsedService(serviceInstance);
                      loginConnections.addHistoryByPort(portForS,
                          "Request for Login, starting Login at: "
                              + getDateTimeNowString());
                      loginConnections.printHistoryByPort(portForS);
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
                      // Login is starting
                    } else if (loginConnections.getNumberOfServices() != 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      requests.add(requestFromQueue);
                      requests.notify();
                      // Login started and is ready to connect
                    } else if (loginConnections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to Login, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_Service_instance_network_address:localhost_"
                          + getServicePort(typeOfService) + ";dest_socket_port:" + getServicePort(typeOfService);
                      writerToAgent.println(dataToSend.replace("agent_to_Manager", "Manager_to_agent"));
                      writerToAgent.flush();
                    }
                    break;
                  case "Post":
                    // Post isn't running
                    if (postsConnections.getNumberOfServices() == 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println(
                          "Manager -> Accepted request to connect to Post, Post is not running");
                      serviceInstance++;
                      // getting port for new instance
                      int portForS = getNextPortForService();
                      // adding entry to Post history
                      postsConnections.addNewConnection(new PostsHistory(portForS, serviceInstance));
                      postsConnections.updateLastUsedService(serviceInstance);
                      postsConnections.addHistoryByPort(portForS,
                          "Request for Post, starting Post at: "
                              + getDateTimeNowString());
                      postsConnections.printHistoryByPort(portForS);
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
                      // Post is starting
                    } else if (postsConnections.getNumberOfServices() != 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      requests.add(requestFromQueue);
                      requests.notify();
                      // Post started and is ready to connect
                    } else if (postsConnections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to Post, sends back data");
                      dataToSend = "type:session_response;" + message_id
                          + ";sub_type:Manager_to_agent;status:200;dest_Service_instance_network_address:localhost_"
                          + getServicePort(typeOfService) + ";dest_socket_port:" + getServicePort(typeOfService);
                      writerToAgent.println(dataToSend.replace("agent_to_Manager", "Manager_to_agent"));
                      writerToAgent.flush();
                    }
                    break;
                  case "Register":
                    // Register isn't running
                    if (registerConnections.getNumberOfServices() == 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println(
                          "Manager -> Accepted request to connect to Register, Register is not running");
                      serviceInstance++;
                      // getting port for new instance
                      int portForS = getNextPortForService();
                      // adding entry to Register history
                      registerConnections.addNewConnection(new RegisterHistory(portForS, serviceInstance));
                      registerConnections.updateLastUsedService(serviceInstance);
                      registerConnections.addHistoryByPort(portForS,
                          "Request for Register, starting Register at: "
                              + getDateTimeNowString());
                      registerConnections.printHistoryByPort(portForS);
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
                      // Register is starting
                    } else if (registerConnections.getNumberOfServices() != 0
                        && requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      requests.add(requestFromQueue);
                      requests.notify();
                      // Register started and is ready to connect
                    } else if (registerConnections.getNumberOfServices() != 0
                        && !requestsForStartServiceList.stream().anyMatch(
                            request -> request.getMessageID() == Integer.parseInt(decodedData[1].split(":")[1]))) {
                      System.out.println("Manager -> Accepted request to connect to Register, sends back data");
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
                int port = Integer.parseInt(decodedData[4].split(":")[1]);
                int destPort = Integer.parseInt(decodedData[5].split(":")[1]);
                // source port is Chat
                if (chatConnections.isServiceWithPort(port)) {
                  // BaaS is the one possible case of connection
                  chatConnections.addHistoryByPort(port,
                      "Chat connected to BaaS at: " + getDateTimeNowString());
                  chatConnections.printHistoryByPort(port);
                  baaSConnections.setConnectedByPort(destPort, true);
                }
                // source port is File
                else if (fileConnections.isServiceWithPort(port)) {
                  // BaaS is the one possible case of connection
                  fileConnections.addHistoryByPort(port,
                      "File connected to BaaS at: " + getDateTimeNowString());
                  fileConnections.printHistoryByPort(port);
                  baaSConnections.setConnectedByPort(destPort, true);
                }
                // source port is Login
                else if (loginConnections.isServiceWithPort(port)) {
                  // BaaS is the one possible case of connection
                  loginConnections.addHistoryByPort(port,
                      "Login connected to BaaS at: " + getDateTimeNowString());
                  loginConnections.printHistoryByPort(port);
                  baaSConnections.setConnectedByPort(destPort, true);
                }
                // source port is Post
                else if (postsConnections.isServiceWithPort(port)) {
                  // BaaS is the one possible case of connection
                  postsConnections.addHistoryByPort(port,
                      "Post connected to BaaS at: " + getDateTimeNowString());
                  postsConnections.printHistoryByPort(port);
                  baaSConnections.setConnectedByPort(destPort, true);
                }
                // source port is Register
                else if (registerConnections.isServiceWithPort(port)) {
                  // BaaS is the one possible case of connection
                  registerConnections.addHistoryByPort(port,
                      "Register connected to BaaS at: " + getDateTimeNowString());
                  registerConnections.printHistoryByPort(port);
                  baaSConnections.setConnectedByPort(destPort, true);
                }
                // source port is Api Gateway
                else {
                  // destination port is Chat
                  if (chatConnections.isServiceWithPort(destPort)) {
                    chatConnections.addHistoryByPort(destPort,
                        "Chat accepted a connection from Api Gateway at: " + getDateTimeNowString());
                    chatConnections.printHistoryByPort(destPort);
                    chatConnections.setConnectedByPort(destPort, true);
                  }
                  // destination port is File
                  else if (fileConnections.isServiceWithPort(destPort)) {
                    fileConnections.addHistoryByPort(destPort,
                        "File accepted a connection from Api Gateway at: " + getDateTimeNowString());
                    fileConnections.printHistoryByPort(destPort);
                    fileConnections.setConnectedByPort(destPort, true);
                  }
                  // destination port is Login
                  else if (loginConnections.isServiceWithPort(destPort)) {
                    loginConnections.addHistoryByPort(destPort,
                        "Login accepted a connection from Api Gateway at: " + getDateTimeNowString());
                    loginConnections.printHistoryByPort(destPort);
                    loginConnections.setConnectedByPort(destPort, true);
                  }
                  // destination port is Post
                  else if (postsConnections.isServiceWithPort(destPort)) {
                    postsConnections.addHistoryByPort(destPort,
                        "Post accepted a connection from Api Gateway at: " + getDateTimeNowString());
                    postsConnections.printHistoryByPort(destPort);
                    postsConnections.setConnectedByPort(destPort, true);
                  }
                  // destination port is Register
                  else if (registerConnections.isServiceWithPort(destPort)) {
                    registerConnections.addHistoryByPort(destPort,
                        "Register accepted a connection from Api Gateway at: " + getDateTimeNowString());
                    registerConnections.printHistoryByPort(destPort);
                    registerConnections.setConnectedByPort(destPort, true);
                  }

                }
                break;

              // source Service closed connetion with another Service
              case "source_service_session_close_info":
                // TODO: add Api Gateway History
                int destPport = Integer.parseInt(decodedData[11].split(":")[1]);
                int sourcePort = Integer.parseInt(decodedData[7].split(":")[1]);
                // source_Service_name:A
                switch (decodedData[3].split(":")[1]) {
                  case "Api Gateway":
                    // dest_Service_name:B
                    switch (decodedData[8].split(":")[1]) {

                      // 1. Api Gateway -> Chat
                      case "Chat":
                        chatConnections.addHistoryByPort(destPport,
                            "Api Gateway send message that closed connection with Chat at: "
                                + getDateTimeNowString());
                        chatConnections.printHistoryByPort(destPport);
                        break;

                      // 2. Api Gateway -> File
                      case "File":
                        fileConnections.addHistoryByPort(destPport,
                            "Api Gateway send message that closed connection with File at: "
                                + getDateTimeNowString());
                        fileConnections.printHistoryByPort(destPport);
                        break;
                      // 3. Api Gateway -> Login
                      case "Login":
                        loginConnections.addHistoryByPort(destPport,
                            "Api Gateway send message that closed connection with Login at: "
                                + getDateTimeNowString());
                        loginConnections.printHistoryByPort(destPport);
                        break;
                      // 4. Api Gateway -> Post
                      case "Post":
                        postsConnections.addHistoryByPort(destPport,
                            "Api Gateway send message that closed connection with Post at: "
                                + getDateTimeNowString());
                        postsConnections.printHistoryByPort(destPport);
                        break;
                      // 5. Api Gateway -> Register
                      case "Register":
                        registerConnections.addHistoryByPort(destPport,
                            "Api Gateway send message that closed connection with Register at: "
                                + getDateTimeNowString());
                        registerConnections.printHistoryByPort(destPport);
                        break;
                      default:
                        System.out.println(
                            "Manager -> In source Service session close info is unknown destination serivce name");
                        break;
                    }
                    break;
                  // Chat -> BaaS
                  case "Chat":
                    chatConnections.addHistoryByPort(sourcePort,
                        "Chat send message that closed connection with BaaS at: " + getDateTimeNowString());
                    chatConnections.printHistoryByPort(sourcePort);
                    baaSConnections.addHistoryByPort(destPport,
                        "Chat send message that closed connection with BaaS at: " + getDateTimeNowString());
                    baaSConnections.printHistoryByPort(destPport);
                    break;
                  // File -> BaaS
                  case "File":
                    fileConnections.addHistoryByPort(sourcePort,
                        "File send message that closed connection with BaaS at: " + getDateTimeNowString());
                    fileConnections.printHistoryByPort(sourcePort);
                    baaSConnections.addHistoryByPort(destPport,
                        "File send message that closed connection with BaaS at: " + getDateTimeNowString());
                    baaSConnections.printHistoryByPort(destPport);
                    break;
                  // Login -> BaaS
                  case "Login":
                    loginConnections.addHistoryByPort(sourcePort,
                        "Login send message that closed connection with BaaS at: " + getDateTimeNowString());
                    loginConnections.printHistoryByPort(sourcePort);
                    baaSConnections.addHistoryByPort(destPport,
                        "Login send message that closed connection with BaaS at: " + getDateTimeNowString());
                    baaSConnections.printHistoryByPort(destPport);
                    break;
                  // Post -> BaaS
                  case "Post":
                    postsConnections.addHistoryByPort(sourcePort,
                        "Post send message that closed connection with BaaS at: " + getDateTimeNowString());
                    postsConnections.printHistoryByPort(sourcePort);
                    baaSConnections.addHistoryByPort(destPport,
                        "Post send message that closed connection with BaaS at: " + getDateTimeNowString());
                    baaSConnections.printHistoryByPort(destPport);
                    break;
                  // Register -> BaaS
                  case "Register":
                    registerConnections.addHistoryByPort(sourcePort,
                        "Register send message that closed connection with BaaS at: " + getDateTimeNowString());
                    registerConnections.printHistoryByPort(sourcePort);
                    baaSConnections.addHistoryByPort(destPport,
                        "Register send message that closed connection with BaaS at: " + getDateTimeNowString());
                    baaSConnections.printHistoryByPort(destPport);
                    break;

                  default:
                    System.out.println("Manager -> Unknown source Service session close info");
                    break;
                }
                break;

              // destination Service closed connetion with another Service
              case "dest_service_session_close_info":
                // dest_Service_name:B
                int ppport = Integer.parseInt(decodedData[10].split(":")[1]);
                switch (decodedData[6].split(":")[1]) {
                  // TODO: add if statements
                  case "BaaS":
                    break;

                  // 1. Chat <- Api Gateway
                  case "Chat":
                    chatConnections.addHistoryByPort(ppport,
                        "Chat send message that closed connection with Api Gateway at: " + getDateTimeNowString());
                    break;
                  // 2. File <- Api Gateway
                  case "File":
                    fileConnections.addHistoryByPort(ppport,
                        "File send message that closed connection with Api Gateway at: " + getDateTimeNowString());
                    break;
                  // 3. Login <- Api Gateway
                  case "Login":
                    fileConnections.addHistoryByPort(ppport,
                        "Login send message that closed connection with Api Gateway at: " + getDateTimeNowString());
                    break;
                  // 4. Post <- Api Gateway
                  case "Post":
                    postsConnections.addHistoryByPort(ppport,
                        "Post send message that closed connection with Api Gateway at: " + getDateTimeNowString());
                    break;
                  // 5. Register <- Api Gateway
                  case "Register":
                    registerConnections.addHistoryByPort(ppport,
                        "Register send message that closed connection with Api Gateway at: " + getDateTimeNowString());
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
                int serviceI = Integer.parseInt(decodedData[4].split(":")[1]);
                switch (decodedData[3].split(":")[1]) {
                  case "BaaS":
                    System.out
                        .println(
                            "Manager -> I got data about that BaaS is processing data, updating lastUsedDateTime");
                    baaSConnections.updateLastUsedService(serviceI);
                    baaSConnections.addHistoryByServiceInstance(serviceI,
                        "BaaS processed data at: " + getDateTimeNowString());
                    baaSConnections.printHistoryByServiceInstance(serviceI);
                    break;
                  case "Chat":
                    System.out
                        .println(
                            "Manager -> I got data about that Chat is processing data, updating lastUsedDateTime");
                    chatConnections.updateLastUsedService(serviceI);
                    chatConnections.addHistoryByServiceInstance(serviceI,
                        "Chat processed data at: " + getDateTimeNowString());
                    chatConnections.printHistoryByServiceInstance(serviceI);
                    break;
                  case "File":
                    System.out
                        .println(
                            "Manager -> I got data about that File is processing data, updating lastUsedDateTime");
                    fileConnections.updateLastUsedService(serviceI);
                    fileConnections.addHistoryByServiceInstance(serviceI,
                        "File processed data at: " + getDateTimeNowString());
                    fileConnections.printHistoryByServiceInstance(serviceI);
                    break;
                  case "Login":
                    System.out
                        .println(
                            "Manager -> I got data about that Login is processing data, updating lastUsedDateTime");
                    loginConnections.updateLastUsedService(serviceI);
                    loginConnections.addHistoryByServiceInstance(serviceI,
                        "Login processed data at: " + getDateTimeNowString());
                    loginConnections.printHistoryByServiceInstance(serviceI);
                    break;
                  case "Post":
                    System.out
                        .println(
                            "Manager -> I got data about that Post is processing data, updating lastUsedDateTime");
                    postsConnections.updateLastUsedService(serviceI);
                    postsConnections.addHistoryByServiceInstance(serviceI,
                        "Post processed data at: " + getDateTimeNowString());
                    postsConnections.printHistoryByServiceInstance(serviceI);
                    break;
                  case "Register":
                    System.out
                        .println(
                            "Manager -> I got data about that Register is processing data, updating lastUsedDateTime");
                    registerConnections.updateLastUsedService(serviceI);
                    registerConnections.addHistoryByServiceInstance(serviceI,
                        "Register processed data at: " + getDateTimeNowString());
                    registerConnections.printHistoryByServiceInstance(serviceI);
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
                // TODO: remove here from serviceHistory not when request is sended
                break;
              case "source_service_session_close_request":
                writerToAgent.println(requestFromQueue);
                writerToAgent.flush();
                break;
              case "source_service_session_close_response":
                System.out.println("Manager ->  Got response: " + requestFromQueue);
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
      case "BaaS":
        return baaSConnections.getPort();
      case "Chat":
        return chatConnections.getPort();
      case "File":
        return fileConnections.getPort();
      case "Login":
        return loginConnections.getPort();
      case "Post":
        return postsConnections.getPort();
      case "Register":
        return registerConnections.getPort();
      default:
        return -1;
    }
  }

  // function that's return next free port for new Service process
  private int getNextPortForService() {
    if (baaSConnections.getNumberOfServices() == 0 && chatConnections.getNumberOfServices() == 0
        && fileConnections.getNumberOfServices() == 0 && loginConnections.getNumberOfServices() == 0
        && postsConnections.getNumberOfServices() == 0 && registerConnections.getNumberOfServices() == 0) {
      int returnedPort = portForService;
      portForService++;
      return returnedPort;
    } else {
      int maxPort = -1;
      int[] tabOfPorts = new int[7];
      if (baaSConnections.getNumberOfServices() != 0)
        tabOfPorts[0] = baaSConnections.getMaxPort();
      else
        tabOfPorts[0] = -1;
      if (chatConnections.getNumberOfServices() != 0)
        tabOfPorts[1] = chatConnections.getMaxPort();
      else
        tabOfPorts[1] = -1;
      if (fileConnections.getNumberOfServices() != 0)
        tabOfPorts[2] = fileConnections.getMaxPort();
      else
        tabOfPorts[2] = -1;
      if (loginConnections.getNumberOfServices() != 0)
        tabOfPorts[3] = loginConnections.getMaxPort();
      else
        tabOfPorts[3] = -1;
      if (postsConnections.getNumberOfServices() != 0)
        tabOfPorts[4] = postsConnections.getMaxPort();
      else
        tabOfPorts[4] = -1;
      if (registerConnections.getNumberOfServices() != 0)
        tabOfPorts[5] = registerConnections.getMaxPort();
      else
        tabOfPorts[5] = -1;
      tabOfPorts[6] = portForService;
      for (int i = 0; i < tabOfPorts.length; i++) {
        if (tabOfPorts[i] > maxPort)
          maxPort = tabOfPorts[i];
      }
      maxPort++;
      return maxPort;
    }
  }
}
