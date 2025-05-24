package TCPServer.lobby;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

import TCPServer.models.Room;
import TCPServer.models.Token;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class LobbyServer {
    public static final int PORT = 12345;
    public static final List<String> active_users = new ArrayList<>();
    public static final Map<String, Room> rooms = new HashMap<>();
    private static final TokenManager tokenManager = new TokenManager();
    private static final String KEYSTORE_FILE = "TCPServer/lobby/server.keystore";
    private static final String KEYSTORE_PASSWORD = "123456";
    private static Map<String, List<String>> chatMessages = new HashMap<>();


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
        String uuid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        tokenManager.addToken(new Token(uuid, username, null,now));
        return uuid;
    }

    public static Token consumeToken(String token, PrintWriter out) {
        for (Token t : tokenManager.getTokens()) {
            if (t.getUuid().equals(token)) {
                if(t.getRoom() == null)return t;
                System.out.println("Token consumed: " + t.toString());
                for (Room r : rooms.values()){
                    if (r.getName().equals(t.getRoom())) {
                        if (!r.isUserInRoom(t.getUsername())) {
                            r.addUser(t.getUsername(), out);
                        }
                        return t;
                    }
                }
                Token newToken = new Token(t.getUuid(), t.getUsername(), null,LocalDateTime.now().plusMinutes(20));
                tokenManager.removeToken(t);
                tokenManager.addToken(newToken);
                return newToken;
                // tokenManager.removeToken(t); // TODO : nao precisamos de remover o token mas ver se queremos refrescar
            }
        }
        return null;
    }

    public static void updateTokenRoom(String token, String room) {
        for (Token t : tokenManager.getTokens()) {
            if (t.getUuid().equals(token)) {
                t.setRoom(room);
                t.setDate(LocalDateTime.now().plusMinutes(20));
                return;
            }
        }
    }

    public static String getFullToken(String uuid){
        for (Token t : tokenManager.getTokens()) {
            if (t.getUuid().equals(uuid)) {
                return t.toString();
            }
        }
        return null;
    }

    public static void addActiveUser(String username) {
    active_users.add(username);
    }

    public static void removeActiveUser(String username) {
        active_users.remove(username);
    }

    public static List<String> getActiveUsers() {
        return new ArrayList<>(active_users); // devolve uma cópia
    }

    public static void printMessage(String message) {
        System.out.println(message);
    }

    public static void addChatRoom(String roomName) {
        chatMessages.put(roomName, new ArrayList<>());
        System.out.println("Number of rooms : "+chatMessages.size());

    }

    public static void addPrompt(String roomName,String prompt) {
        chatMessages.get(roomName).add(prompt);
        System.out.println("Number of messages in this room: " + chatMessages.get(roomName).size());
    }

    public static String getMessages(String roomName) {
            for (Map.Entry<String, List<String>> entry : chatMessages.entrySet()) {
                if (entry.getKey().equals(roomName)) {
                    // Join all messages with newlines or another delimiter
                    System.out.println(String.join(",\n", entry.getValue()));
                    return String.join(",\n", entry.getValue());
                }
            }
            return ""; // or null, or throw exception if not found
    }

}
