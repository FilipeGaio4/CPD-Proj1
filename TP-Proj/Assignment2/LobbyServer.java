package Assignment2;

import java.io.*;
import java.net.*;
import java.util.*;

public class LobbyServer {
    private static final int PORT = 12345;
    private static final String USER_FILE = "Assignment2/users.txt";
    private static Map<String, String> userCredentials = new HashMap<>();
    private static Map<String, Integer> rooms = new HashMap<>(); // nome da sala -> porta
    private static Map<String, String> activeTokens = new HashMap<>();

    
    public static void main(String[] args) {
        loadUsers();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Lobby server started on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientLobbyHandler(clientSocket)).start();
            }
            
        } catch (IOException e) {
            System.out.println("Lobby server error: " + e.getMessage());
        }
    }
    
    private static String generateToken(String username) {
        String token = UUID.randomUUID().toString();
        activeTokens.put(token, username);
        return token;
    }

    public static String consumeToken(String token) {
        return activeTokens.remove(token); // remove e devolve o username (ou null)
    }

    private static void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    userCredentials.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user file: " + e.getMessage());
        }
    }

    private static class ClientLobbyHandler implements Runnable {
        private Socket socket;
        private String username;

        public ClientLobbyHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                out.println("Welcome to the chat system! Please log in.");
                out.print("Username: ");
                out.flush();
                username = in.readLine();
                out.print("Password: ");
                out.flush();
                String password = in.readLine();

                if (!authenticate(username, password)) {
                    out.println("Authentication failed. Closing connection.");
                    socket.close();
                    return;
                }

                out.println("Authentication successful. Welcome, " + username + "!");
                handleMenu(out, in, username);

            } catch (IOException e) {
                System.out.println("Error handling lobby client: " + e.getMessage());
            }
        }

        private boolean authenticate(String username, String password) {
            return userCredentials.containsKey(username) && userCredentials.get(username).equals(password);
        }

        private void handleMenu(PrintWriter out, BufferedReader in, String username) throws IOException {
            while (true) {
                out.println("\n--- MENU ---");
                out.println("Available rooms:");
                synchronized (rooms) {
                    if (rooms.isEmpty()) {
                        out.println("(No rooms yet)");
                    } else {
                        for (String room : rooms.keySet()) {
                            out.println("- " + room);
                        }
                    }
                }
                out.println("\n1 - Join a room");
                out.println("2 - Create a new room");
                out.println("3 - Quit");
                out.print("Choice: ");
                out.flush();

                String choice = in.readLine();
                switch (choice) {
                    case "1":
                        if(joinRoom(out, in)) {
                            socket.close();
                            return;
                        } 
                        break;
                    case "2":
                        createRoom(out, in);
                        break;
                    case "3":
                        out.println("Goodbye!");
                        socket.close();
                        return;
                    default:
                        out.println("Invalid option.");
                }
            }
        }

        private boolean joinRoom(PrintWriter out, BufferedReader in) throws IOException {
            out.print("Enter room name: ");
            out.flush();
            String roomName = in.readLine();

            // TODO : Passado algum tempo podemos apagar o token para n ficar infinitamente
            synchronized (rooms) {
                if (rooms.containsKey(roomName)) {
                    int port = rooms.get(roomName);
                    String token = generateToken(username); 
                    out.println("Connect to room '" + roomName + "' on port " + port + " with token:" + token);
                    out.println("Usage:");
                    out.println("telnet localhost " + port);
                    out.println("When asked provide token:");
                    out.println(token);
                    return true;
                } else {
                    out.println("Room does not exist.");
                    return false;
                }
            }
        }

        private void createRoom(PrintWriter out, BufferedReader in) throws IOException {
            out.print("Enter new room name: ");
            out.flush();
            String roomName = in.readLine();

            synchronized (rooms) {
                if (rooms.containsKey(roomName)) {
                    out.println("Room already exists.");
                    return;
                }

                int port = getAvailablePort();
                rooms.put(roomName, port);
                new Thread(() -> RoomServer.launchRoom(roomName, port)).start();
                out.println("Room '" + roomName + "' created on port " + port + ".");
            }
        }

        private int getAvailablePort() {
            return 15000 + rooms.size();
        }
    }
}
