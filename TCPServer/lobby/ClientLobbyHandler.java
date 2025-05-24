package TCPServer.lobby;

import TCPServer.models.*;
import TCPServer.lobby.LobbyServer; 
import TCPServer.lobby.AuthManager;

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
    private enum ClientState {
        UNDEFINED,
        AUTHENTICATION,
        LOBBY,
        ROOM
    }
    private ClientState client_state = ClientState.UNDEFINED;

    public ClientLobbyHandler(Socket socket) {
        this.socket = socket;
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public void run() {
        try {
            setupIO();
            login();
            if (client_state == ClientState.ROOM){
                sendMessage(":resume");
                sendMessage("You are currently in room " + currentRoom.getName() + ".");
                sendMessage(":room_help");
                chatLoop();
            }
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
                        sendMessage(":goodbye");
                        return;
                    default:
                        sendMessage("Invalid option.");
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
        if (choice.equals("1")){
            username = in.readLine();
            System.out.println("Logging in user: " + username);
            String pwd = in.readLine();

            if (!AuthManager.authenticate(username, pwd)) {
                sendMessage("Authentication failed. Disconnecting.");
                throw new IOException("Auth failed");
            }
        }
        else if (choice.equals("2")) {          // Token Login
            token_uuid = in.readLine();
            System.out.println("Token: " + token_uuid);
            Token token = LobbyServer.consumeToken(token_uuid, out);
            username = token.getUsername();
            if (username == null) {
                sendMessage("Invalid token. Disconnecting.");
                throw new IOException("Invalid token");
            }
            ClientState state = token.getRoom() != null ? ClientState.ROOM : ClientState.LOBBY;
            Room room = LobbyServer.rooms.get(token.getRoom()); // Double checking beacuse im already checking in consume token
            if (room == null) {
                sendMessage(":deleted_room");
            }
            // Puts the user in the room even if its null
            currentRoom = room;
            change_state(state);
        }
        else if (choice.equals("3")) {          // Register
            username = in.readLine();
            System.out.println("Registering user: " + username);
            String pwd = in.readLine();
            if (!AuthManager.register(username, pwd)) {
                sendMessage("Registration failed. Disconnecting.");
                throw new IOException("Reg failed");
            }
        }
        else {
            sendMessage("Invalid option. Disconnecting.");
            throw new IOException("Invalid option");
        }

        if (LobbyServer.active_users.contains(username)) {
            sendMessage("User already logged in. Disconnecting.");
            throw new IOException("User already logged in somewhere");
        }
        LobbyServer.printMessage("User " + username + " logged in.");
        sendMessage("Authenticated as " + username);
        if (choice.equals("1") || choice.equals("3")) {
            token_uuid = LobbyServer.generateToken(username);
            sendMessage("Your token: " + token_uuid);
        }
        else {
            sendMessage("If needed again, your token: " + token_uuid);
        }
        LobbyServer.addActiveUser(username);
    }

    private void change_state(ClientState state) throws IOException {
        sendMessage(":change_state " + state);
        client_state = state;
    }

    private void menu() throws IOException {
        sendMessage(":menu");
    }


    private boolean joinRoom() throws IOException {
        System.out.println("Rooms: " + LobbyServer.rooms.keySet());
        sendMessage("\n--- Available rooms ---");
        synchronized (LobbyServer.rooms) { // TODO : change here to our lock
            if (LobbyServer.rooms.keySet().size() == 0) {
                sendMessage(":no_rooms");
            }else {
                String rooms_list = "";
                for (var i : LobbyServer.rooms.keySet()) {
                    rooms_list += "\n  -" + i;
                }
                sendMessage(rooms_list);
            }
        }
        sendMessage("Enter a Room Name: ");
        String roomName = in.readLine();
        LobbyServer.printMessage("Room Name: " + roomName);
        Room room = LobbyServer.rooms.get(roomName);
        if (room == null) {
            sendMessage(roomName + " does not exist.");
            return false;
        }
        // associa o cliente à sala
        currentRoom = room;
        room.addUser(username, out);
        // atualiza o token com a sala
        LobbyServer.updateTokenRoom(token_uuid, roomName);
        // out.println("Updated token: " + LobbyServer.getFullToken(token_uuid));
        sendMessage("Joined room " + roomName);
        sendMessage(":room_help");

        LobbyServer.printMessage("User " + username + " joined room " + roomName);
        return true;
    }

    private void createRoom() throws IOException {
        sendMessage("Enter a Room Name: ");
        String roomName = in.readLine();
        System.out.println("Room Name: " + roomName);
        synchronized (LobbyServer.rooms) { // TODO : change here to our lock
            if (LobbyServer.rooms.containsKey(roomName)) {
                sendMessage("Room already exists.");
                return;
            }if(roomName.equals("")){
                sendMessage("Insert a name please.");
                return;
            }
            if (!roomName.matches("[A-Za-z0-9_]+")) {
                sendMessage("Invalid room name. Only alphanumeric characters and underscores are allowed.");
                return;
            }
            Room room = new Room(roomName);
            LobbyServer.rooms.put(roomName, room);
            LobbyServer.printMessage("Room " + roomName + " created by " + username);
            LobbyServer.addChatRoom(roomName);
            sendMessage("Room '" + roomName + "' created.");
        }
    }

    private void chatLoop() throws IOException, InterruptedException {
        String msg;
        while ((msg = in.readLine()) != null) {
            if (msg.equalsIgnoreCase(":q")) {
                currentRoom.removeUser(username);
                currentRoom = null;
                return;
            }else if (msg.startsWith(":ai")) {
                String[] parts = msg.split(" ", 2);
                if (parts.length < 2) {
                    sendMessage("Usage: :ai <message>");
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
                sendMessage("Response from AI model:\n" + content);
            } else if (msg.equalsIgnoreCase(":logout")){
                cleanup();
            }
            else if (msg.equalsIgnoreCase(":u")) {
                synchronized (currentRoom) { // TODO : change here to our lock
                    currentRoom.listUsers(out);

                }
            }
            else if (msg.startsWith(":m ")) {
                String[] parts = msg.split(" ", 3);
                if (parts.length < 3) {
                    sendMessage("Usage: :m <username> <message>");
                    continue;
                }
                String receiver = parts[1];
                String privateMessage = parts[2];
                currentRoom.broadcast("[" + username + "] (private): " + privateMessage, receiver);
            }
            else if (msg.equalsIgnoreCase(":h")) {  // This will never be used since client is not sending :h
                sendMessage("RULES and Shortcuts:");
                sendMessage("- ':q' to leave the room.");
                sendMessage("- ':u' to list users.");
                sendMessage("- ':m <username> <message>' to send a private message.");
                sendMessage("- ':h' to see this help.");
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
        try { socket.close(); } catch (IOException ignored) {}
        LobbyServer.removeActiveUser(username);
        if (currentRoom != null) {
            currentRoom.removeUser(username);
        }
    }

    private void sendMessage(String message) {
        out.println(message);
        out.flush();
    }
}
