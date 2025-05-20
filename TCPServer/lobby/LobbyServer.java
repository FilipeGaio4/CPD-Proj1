package TCPServer.lobby;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

import TCPServer.models.Room;
import TCPServer.models.Token;

public class LobbyServer {
    public static final int PORT = 12345;
    public static final Map<String, Room> rooms = new HashMap<>();
    private static final TokenManager tokenManager = new TokenManager();

    public static void main(String[] args) {
        AuthManager.loadUsers();

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

    public static String generateToken(String username) {
        String uuid = UUID.randomUUID().toString();
        tokenManager.addToken(new Token(uuid, username, null));
        return uuid;
    }

    public static String consumeToken(String token) {
        for (Token t : tokenManager.getTokens()) {
            if (t.getUuid().equals(token)) {
                tokenManager.removeToken(t);
                return t.getUsername();
            }
        }
        return null;
    }

    public static void updateTokenRoom(String token, String room) {
        for (Token t : tokenManager.getTokens()) {
            if (t.getUuid().equals(token)) {
                t.setRoom(room);
                return;
            }
        }
    }

    public static void printMessage(String message) {
        System.out.println(message);
    }
}
