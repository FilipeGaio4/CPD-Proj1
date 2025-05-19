package TCPServer.lobby;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import TCPServer.models.Room;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class LobbyServer {
    public static final int PORT = 12345;
    public static final Map<String, Room> rooms = new HashMap<>();
    private static final Map<String, String> activeTokens = new HashMap<>();
    private static final String KEYSTORE_FILE = "TCPServer/lobby/server.keystore";
    private static final String KEYSTORE_PASSWORD = "123456";

    public static void main(String[] args) {
        AuthManager.loadUsers();
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_FILE);
        System.setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_PASSWORD);
        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try (SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(PORT)) {
            System.out.println("Lobby server started on port " + PORT);

            while (true) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                new Thread(new ClientLobbyHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            System.out.println("Lobby server error: " + e.getMessage());
        }
    }

    public static String generateToken(String username) {
        String token = UUID.randomUUID().toString();
        activeTokens.put(token, username);
        return token;
    }

    public static String consumeToken(String token) {
        return activeTokens.remove(token); // devolve username ou null
    }

    public static void printMessage(String message) {
        System.out.println(message);
    }
}
