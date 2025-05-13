import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static int PORT = 12345;
    private static String PASSWORD = "securepassword";
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        PASSWORD = args.length > 0 ? args[0] : PASSWORD;
        PORT = args.length > 1 ? Integer.parseInt(args[1]) : PORT;
        System.out.println("Usage: java ChatServer <password> <port> or else default values");
        System.out.println("Chat server started on port " + PORT + " with password: " + PASSWORD);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    static void broadcast(String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Authentication
                out.println("Enter password:");
                String password = in.readLine();
                if (!PASSWORD.equals(password)) {
                    out.println("Authentication failed. Disconnecting...");
                    socket.close();
                    return;
                }
                out.println("Authentication successful. Enter your name:");
                clientName = in.readLine();
                out.println("Welcome to the chat, " + clientName + "!");
                broadcast(clientName + " has joined the chat.", this);

                // Chat loop
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("QUIT")) {
                        break;
                    }
                    broadcast(clientName + ": " + message, this);
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
                broadcast(clientName + " has left the chat.", this);
                removeClient(this);
            }
        }

        void sendMessage(String message) {
            out.println(message);
        }
    }
}