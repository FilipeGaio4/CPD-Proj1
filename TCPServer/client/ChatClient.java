package TCPServer.client;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final String TRUSTSTORE_FILE = "TCPServer/client/client.truststore";  // Truststore file containing server's public certificate
    private static final String TRUSTSTORE_PASSWORD = "123456";
    private static PrintWriter out;
    private static Scanner scanner;
    private enum ClientState {
        UNDEFINED,
        AUTHENTICATION,
        LOBBY,
        ROOM
    }
    private static String username;
    private static ClientState client_state = ClientState.UNDEFINED;
    private static final Map<String, String> serverMessages = new HashMap<>();
    static {
        serverMessages.put(":no_rooms", "(No rooms available)");
        serverMessages.put(":menu", "\n--- MENU ---\n1 - Join a room\n2 - Create a new room\n3 - Quit\nChoice: ");
        serverMessages.put(":goodbye", "\nGoodbye!\nPress Enter to exit.");
        serverMessages.put(":resume", "\nResuming your session with token.");
        serverMessages.put(":deleted_room", "\nThe room you were in no longer exists. Please create or join a new room.");
        serverMessages.put(":room_help", "\nShortcuts:\n- ':q' to leave the room.\n- ':u' to list users.\n- ':m <username> <message>' to send a private message.\n- ':h' to see this help.\n- ':logout' to log out.");
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE);
        System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);

        // Create SSL socket
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(SERVER_HOST, SERVER_PORT);) {
            System.out.println("Connected to the chat server.");
            new Thread(new ReadHandler(socket)).start();

            // Initialize input and output streams
            out = new PrintWriter(socket.getOutputStream(), true);
            scanner = new Scanner(System.in);
            
            // Authentication
            client_state = ClientState.AUTHENTICATION;
            login();

            // Chat loop
            String message;
            while (true) {
                if (socket.isClosed()) {
                    System.out.println("This socket is closed.");
                    scanner.close();
                    return;
                }
                message = scanner.nextLine();
                if (message.equalsIgnoreCase(":logout")) {
                    out.println(":logout");
                    break;
                }
                else if (message.equalsIgnoreCase(":h")) {
                    System.out.println(serverMessages.get(":room_help"));
                }
                else {
                    out.println(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void login() throws IOException {
        System.out.println("Choose login method:");
        System.out.println("1 - Login with username and password");
        System.out.println("2 - Login with token");
        System.out.println("3 - Register new user");
        String choice = scanner.nextLine();
        out.println(choice);
        client_state = ClientState.LOBBY; // Set state to lobby by default, if room server will send
        if (choice.equals("1")) {
            System.out.println("Enter Username:");
            username = scanner.nextLine();
            out.println(username);
            System.out.println("Enter Password:");
            out.println(scanner.nextLine());
        } else if (choice.equals("2")) {
            System.out.println("Enter your token:");
            out.println(scanner.nextLine());
        } else if (choice.equals("3")) {
            System.out.println("Enter New Username:");
            username = scanner.nextLine();
            out.println(username);
            System.out.println("Enter New Password:");
            out.println(scanner.nextLine());
        } else {
            System.out.println("Invalid option. Disconnecting.");
            throw new IOException("Invalid option");
        }
    }

    private static class ReadHandler implements Runnable {
        private Socket socket;

        public ReadHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("[" + username + "]")){
                        message = "[You]" + message.substring(message.indexOf("]") + 1);
                        System.out.println(message);
                    }
                    else if (serverMessages.containsKey(message.trim())) {
                        System.out.println(serverMessages.get(message.trim()));
                    } 
                    else if (message.startsWith(":change_state")) {
                        String[] parts = message.split(" ");
                        if (parts.length > 1) {
                            String state = parts[1];
                            switch (state) {
                                case "AUTHENTICATION":
                                    client_state = ClientState.AUTHENTICATION;
                                    break;
                                case "LOBBY":
                                    client_state = ClientState.LOBBY;
                                    break;
                                case "ROOM":
                                    client_state = ClientState.ROOM;
                                    break;
                                default:
                                    System.out.println("Unknown state: " + state);
                            }
                        }
                        System.out.println("Server command: " + message);
                    } else {
                        System.out.println(message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
            }
        }
    }
}
