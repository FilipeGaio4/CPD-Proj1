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
    private enum ClientState {
        UNDEFINED,
        AUTHENTICATION,
        LOBBY,
        ROOM
    }
    private static ClientState clientState = ClientState.UNDEFINED;
    private static final Map<String, String> serverMessages = new HashMap<>();
    static {
        serverMessages.put(":no_rooms", "(No rooms available)");
        serverMessages.put(":menu", "\n--- MENU ---\n1 - Join a room\n2 - Create a new room\n3 - Quit\nChoice: ");
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE);
        System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);

        // Create SSL socket
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(SERVER_HOST, SERVER_PORT);) {
            System.out.println("Connected to the chat server.");
            new Thread(new ReadHandler(socket)).start();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
            
            // Authentication
            clientState = ClientState.AUTHENTICATION;
            System.out.println("Enter Username:");
            out.println(scanner.nextLine());
            System.out.println("Enter Password:");
            out.println(scanner.nextLine());

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
                out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
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
                    if (serverMessages.containsKey(message.trim())) {
                        System.out.println(serverMessages.get(message.trim()));
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
