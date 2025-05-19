package Assignment2;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.*;
import java.util.*;

public class LobbyServer {
    private static final int PORT = 12345;
    private static final String USER_FILE = "Assignment2/users.txt";
    private static Map<String, String> userCredentials = new HashMap<>();
    private static Map<String, Integer> rooms = new HashMap<>(); // nome da sala -> porta
    private static Map<String, String> activeTokens = new HashMap<>();
    private static final String KEYSTORE_FILE = "Assignment2/server.keystore";
    private static final String KEYSTORE_PASSWORD = "123456";


    public static void main(String[] args) {
        loadUsers();
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_FILE);
        System.setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_PASSWORD);
        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        // Create SSL Server Socket on the port
        try (SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(PORT);) {
            System.out.println("Lobby server started on port " + PORT);

            while (true) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
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
        private SSLSocket socket;
        private String username;

        public ClientLobbyHandler(SSLSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                System.out.println("Waiting for data about User...");
                username = in.readLine();
                System.out.println(username);

                String password = in.readLine();
                System.out.println(password);

                if (!authenticate(username, password)) {
                    out.println("Authentication failed. Closing connection.");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.out.println("Error while closing socket: " + e.getMessage());
                    }
                    return;
                }

                out.println("Authentication successful. Welcome, " + username + "!");
                handleMenu(out, in);

            } catch (IOException e) {
                System.out.println("Error handling lobby client: " + e.getMessage());
            }
        }

        private boolean authenticate(String username, String password) {
            return userCredentials.containsKey(username) && userCredentials.get(username).equals(password);
        }

        private void handleMenu(PrintWriter out, BufferedReader in) throws IOException {
            while (true) {
                System.out.println("Beginning");
                synchronized (rooms) {
                    if (rooms.isEmpty()) {
                        out.println("(No rooms yet)");
                        out.flush();
                    } else {
                        out.println(rooms.keySet());
                    }
                }


                String choice = in.readLine();
                System.out.println("The user chose the option: "+choice);
                switch (choice) {
                    case "1":
                        if (!joinRoom(out, in)) {
                            socket.close();
                            return;
                        }
                        System.out.println("Joining room...");
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
;
            String roomName = in.readLine();
            System.out.println("Received room name: " + roomName);

            // TODO : Passado algum tempo podemos apagar o token para n ficar infinitamente
            synchronized (rooms) {
                if (rooms.containsKey(roomName)) {
                    int port = rooms.get(roomName);
                    String token = generateToken(username);
                    out.println(token);
                    out.flush();
                    out.println(port);
                    out.flush();
                    System.out.println("AIIIIIII");
                    return true;
                } else {
                    out.println("Room does not exist.");
                    out.flush();
                    return false;
                }
            }
        }

        private void createRoom(PrintWriter out, BufferedReader in) throws IOException {

            String roomName = in.readLine();
            System.out.println("Received room name: " + roomName);


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
