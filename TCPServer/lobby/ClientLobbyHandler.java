package TCPServer.lobby;

import TCPServer.models.*;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class ClientLobbyHandler implements Runnable {
    private final Socket socket;
    private String username;
    private PrintWriter out;
    private BufferedReader in;
    private Room currentRoom;
    private String token_uuid;
    private HttpClient client;

    public ClientLobbyHandler(Socket socket) {
        this.socket = socket;
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public void run() {
        try {
            setupIO();
            login();
            while (true) {
                menu();
                String choice = in.readLine();
                if (choice == null) break;

                switch (choice) {
                    case "1":
                        if (joinRoom()) {
                            chatLoop();
                        }
                        break;
                    case "2":
                        createRoom();
                        break;
                    case "3":
                        out.println(":goodbye");
                        return;
                    default:
                        out.println("Invalid option.");
                }
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }


    private void setupIO() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void login() throws IOException { // TODO : allow register but becareful with names. .matches("[A-Za-z0-9_]+") and not you
        String choice = in.readLine();
        if (choice.equals("1")) {
            username = in.readLine();
            System.out.println("Logging in user: " + username);
            String pwd = in.readLine();

            if (!AuthManager.authenticate(username, pwd)) {
                out.println("Authentication failed. Disconnecting.");
                throw new IOException("Auth failed");
            }
        } else if (choice.equals("2")) {
            String token_uuid = in.readLine();
            System.out.println("Token: " + token_uuid);
            Token token = LobbyServer.consumeToken(token_uuid);
            username = token.getUsername();
            if (username == null) {
                out.println("Invalid token. Disconnecting.");
                throw new IOException("Invalid token");
            }
            String room = token.getRoom() != null ? "ROOM" : "LOBBY";
            change_state(room);
        } else {
            out.println("Invalid option. Disconnecting.");
            throw new IOException("Invalid option");
        }

        if (LobbyServer.active_users.contains(username)) {
            out.println("User already logged in. Disconnecting.");
            throw new IOException("User already logged in somewhere");
        }
        LobbyServer.printMessage("User " + username + " logged in.");
        out.println("Authenticated as " + username);
        out.flush();
        if (choice.equals("1")) {
            token_uuid = LobbyServer.generateToken(username);
            out.println("Your token: " + token_uuid);
            out.flush();
        }
        LobbyServer.addActiveUser(username);
    }

    private void change_state(String state) throws IOException {
        out.println(":change_state " + state);
        out.flush();
    }

    private void menu() throws IOException {
        out.println(":menu");
        out.flush();
    }


    private boolean joinRoom() throws IOException {
        System.out.println("Rooms: " + LobbyServer.rooms.keySet());
        out.println("\n--- Available rooms ---");
        synchronized (LobbyServer.rooms) { // TODO : change here to our lock
            if (LobbyServer.rooms.keySet().size() == 0) {
                out.println(":no_rooms");

            } else {
                String rooms_list = "";
                for (var i : LobbyServer.rooms.keySet()) {
                    rooms_list += "\n  -" + i;
                }
                out.println(rooms_list);
            }
        }
        out.println("Enter a Room Name: ");
        out.flush();
        String roomName = in.readLine();
        LobbyServer.printMessage("Room Name: " + roomName);
        Room room = LobbyServer.rooms.get(roomName);
        if (room == null) {
            out.println("Room does not exist.");
            return false;
        }
        // associa o cliente à sala
        currentRoom = room;
        room.addUser(username, out);
        // atualiza o token com a sala
        LobbyServer.updateTokenRoom(token_uuid, roomName);
        out.println("Updated token: " + LobbyServer.getFullToken(token_uuid));
        out.println("Joined room " + roomName);
        out.flush();

        LobbyServer.printMessage("User " + username + " joined room " + roomName);
        return true;
    }

    private void createRoom() throws IOException {
        out.println("Enter a Room Name: ");
        out.flush();
        String roomName = in.readLine();
        System.out.println("Room Name: " + roomName);
        synchronized (LobbyServer.rooms) { // TODO : change here to our lock
            if (LobbyServer.rooms.containsKey(roomName)) {
                out.println("Room already exists.");
                out.flush();
                return;
            }
            if (roomName.equals("")) {
                out.println("Insert a name please.");
                out.flush();
                return;
            }
            if (!roomName.matches("[A-Za-z0-9_]+")) {
                out.println("Invalid room name. Only alphanumeric characters and underscores are allowed.");
                out.flush();
                return;
            }
            Room room = new Room(roomName);
            LobbyServer.rooms.put(roomName, room);
            LobbyServer.printMessage("Room " + roomName + " created by " + username);
            LobbyServer.addChatRoom(roomName);
            out.println("Room '" + roomName + "' created.");
            out.flush();
        }
    }

    private void chatLoop() throws IOException, InterruptedException {
        String msg;
        while ((msg = in.readLine()) != null) {
            if (msg.equalsIgnoreCase(":q")) {
                currentRoom.removeUser(username);
                currentRoom = null;
                return;
            } else if (msg.startsWith(":ai")) {
                String[] parts = msg.split(" ", 2);
                if (parts.length < 2) {
                    out.println("Usage: :ai <message>");
                    continue;
                }
                currentRoom.broadcast("[" + username + "]: " + msg);
                String promptContent = "User asking the question to the ai: " + username + ", Message: " + parts[1];

                String userMessage = """
                            {
                              "role": "user",
                              "content": "%s"
                            }
                        """.formatted(promptContent);

                // Append this message to the room's chat history
                LobbyServer.addPrompt(currentRoom.getName(), userMessage);

                String allMessages = LobbyServer.getMessages(currentRoom.getName());

                // Final JSON payload for Ollama
                String json = """
                            {
                              "model": "llama3",
                              "messages": [%s],
                              "stream": false,
                              "format": "json"
                            }
                        """.formatted(allMessages);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:11434/api/chat"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                int startIndex = response.body().indexOf("\"content\":\"{");
                startIndex += "\"content\":\"".length();
                int braceCount = 0;
                StringBuilder content = new StringBuilder();

                for (int i = startIndex; i < response.body().length(); i++) {
                    char c = response.body().charAt(i);
                    content.append(c);

                    if (c == '{') braceCount++;
                    else if (c == '}') braceCount--;

                    if (braceCount == 0) {
                        break;
                    }
                }
                System.out.println("Extracted content: " + content);
                System.out.println(response.body());
                //String answer = "{ \"role\": \"assistant\", \"content\": \"" + content + "\" }";
                String answer = """
                    {
                      "role": "assistant",
                      "content": "%s"
                    }
                """.formatted(content);
                LobbyServer.addPrompt(currentRoom.getName(), answer);
                out.println("Response from AI model:\n" + content);
            } else if (msg.equalsIgnoreCase(":u")) {
                synchronized (currentRoom) { // TODO : change here to our lock
                    currentRoom.listUsers(out);

                }
            } else if (msg.startsWith(":m ")) {
                String[] parts = msg.split(" ", 3);
                if (parts.length < 3) {
                    out.println("Usage: :m <username> <message>");
                    continue;
                }
                String receiver = parts[1];
                String privateMessage = parts[2];
                currentRoom.broadcast("[" + username + "] (private): " + privateMessage, receiver);
            } else if (msg.equalsIgnoreCase(":h")) {
                out.println("RULES and Shortcuts:");
                out.println("- ':q' to leave the room.");
                out.println("- ':u' to list users.");
                out.println("- ':m <username> <message>' to send a private message.");
                out.println("- ':h' to see this help.");
            } else if (msg.isEmpty()) {
                continue;
            } else {

                System.out.println("BROADCAST: " + msg);
                currentRoom.broadcast("[" + username + "]: " + msg);
                msg = "User talking to the other users: " + username + ", " + msg;
                msg = """  
                        { "role": "user", "content":""" + "\"" + msg + "\"" + """
                        }""";
                LobbyServer.addPrompt(currentRoom.getName(), msg);
            }
        }
    }

    private void cleanup() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        LobbyServer.removeActiveUser(username);
        if (currentRoom != null) {
            currentRoom.removeUser(username);
        }
    }
}
