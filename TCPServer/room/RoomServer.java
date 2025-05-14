// package TCPServer.room;

// import java.io.*;
// import java.net.*;
// import java.util.*;
// import TCPServer.lobby.LobbyServer;

// // Not being used so far

// public class RoomServer {
//     private final int port;
//     private final String roomName;
//     private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>()); // TODO : verificar se pudemos usar isto ou mudar para o nosso lock

//     public RoomServer(String roomName, int port) {
//         this.roomName = roomName;
//         this.port = port;
//     }

//     public void start() {
//         System.out.println("Starting room '" + roomName + "' on port " + port);
//         try (ServerSocket serverSocket = new ServerSocket(port)) {
//             System.out.println("Room '" + roomName + "' is now active on port " + port);

//             while (true) {
//                 Socket clientSocket = serverSocket.accept();
//                 System.out.println("New client joined room '" + roomName + "' from " + clientSocket.getInetAddress());

//                 ClientHandler handler = new ClientHandler(clientSocket);
//                 clients.add(handler);
//                 new Thread(handler).start();
//             }

//         } catch (IOException e) {
//             System.out.println("RoomServer error on port " + port + ": " + e.getMessage());
//         }
//     }

//     private void broadcast(String message, ClientHandler sender) {
//         synchronized (clients) {
//             for (ClientHandler client : clients) {
//                 if (client != sender) {
//                     client.sendMessage(message);
//                 }
//             }
//         }
//     }

//         private void broadcast(String message, ClientHandler sender, String receiver) {
//         synchronized (clients) {
//             for (ClientHandler client : clients) {
//                 if (client != sender && client.clientName.equals(receiver)) {
//                     client.sendMessage(message);
//                 }
//             }
//         }
//     }

//     private class ClientHandler implements Runnable {
//         private Socket socket;
//         private PrintWriter out;
//         private BufferedReader in;
//         private String clientName;

//         public ClientHandler(Socket socket) {
//             this.socket = socket;
//         }

//         public void run() {
//             try {
//                 out = new PrintWriter(socket.getOutputStream(), true);
//                 in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

//                 out.println("Enter your token:");
//                 String token = in.readLine();
//                 clientName = LobbyServer.consumeToken(token);

//                 if (clientName == null) {
//                     out.println("Invalid or expired token.");
//                     socket.close();
//                     return;
//                 }else {
//                     out.println("Welcome to the room, " + clientName + "!");
//                     out.println("RULES and Shortcuts:");
//                     out.println("\t- ':q' to leave the room.");
//                     out.println("\t- ':u' to list users.");
//                     out.println("\t- ':m <username> <message>' to send a private message.");
//                     out.println("\t- ':h' to see this help.");
//                 }

//                 broadcast(clientName + " joined the room.", this);

//                 String message;
//                 while ((message = in.readLine()) != null) {
//                     if (message.equalsIgnoreCase(":q")) break;
//                     if (message.equalsIgnoreCase(":u")) {
//                         out.println("Users in the room:");
//                         synchronized (clients) {
//                             for (ClientHandler client : clients) {
//                                 out.println("- " + client.clientName);
//                             }
//                         }
//                         continue;
//                     }
//                     else if (message.equalsIgnoreCase(":h")) {
//                         out.println("RULES and Shortcuts:");
//                         out.println("\t- ':q' to leave the room.");
//                         out.println("\t- ':u' to list users.");
//                         out.println("\t- ':m <username> <message>' to send a private message.");
//                         out.println("\t- ':h' to see this help.");
//                         continue;
//                     }
//                     else if (message.startsWith(":m ")) {
//                         String[] parts = message.split(" ", 3);
//                         if (parts.length < 3) {
//                             out.println("Usage: :m <username> <message>");
//                             continue;
//                         }
//                         String receiver = parts[1];
//                         String privateMessage = parts[2];
//                         broadcast(clientName + " (private): " + privateMessage, this, receiver);
//                         continue;
//                     }
//                     else {
//                         broadcast(clientName + ": " + message, this);
//                     }
//                 }
//             } catch (IOException e) {
//                 System.out.println("Client error: " + e.getMessage());
//             } finally {
//                 try {
//                     socket.close();
//                 } catch (IOException ignored) {}

//                 clients.remove(this);
//                 if (clientName != null) {
//                     broadcast(clientName + " left the room.", this);
//                 }
//             }
//         }

//         void sendMessage(String message) {
//             out.println(message);
//         }
//     }

//     public static void launchRoom(String roomName, int port) {
//         RoomServer server = new RoomServer(roomName, port);
//         server.start();
//     }
// }
