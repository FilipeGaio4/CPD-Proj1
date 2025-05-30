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
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
            if (client_state == ClientState.ROOM) {
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

    private void login() throws IOException { 
        String choice = in.readLine();
        if (choice.equals("1")) {
            username = in.readLine();
            System.out.println("Logging in user: " + username);
            String pwd = in.readLine();

            if (!AuthManager.authenticate(username, pwd)) {
                sendMessage("Authentication failed. Disconnecting.");
                throw new IOException("Auth failed");
            }
        } else if (choice.equals("2")) {          // Token Login
            token_uuid = in.readLine();
            System.out.println("Token: " + token_uuid);
            Token token = LobbyServer.consumeToken(token_uuid, out);
            username = token.getUsername();
            if (username == null) {
                sendMessage("Invalid or expired token. Disconnecting.");
                throw new IOException("Invalid token");
            } else if (LobbyServer.active_users.contains(username)){ // Extra verification here
                sendMessage("User already logged in. Disconnecting.");
                throw new IOException("User already logged in");
            }
            ClientState state = token.getRoom() != null ? ClientState.ROOM : ClientState.LOBBY;
            Room room = LobbyServer.rooms.get(token.getRoom()); // Double checking beacuse im already checking in consume token
            // Puts the user in the room even if its null
            if (room == null) {
                sendMessage(":no_room");
            }
            currentRoom = room;
            change_state(state);
        } else if (choice.equals("3")) {          // Register
            username = in.readLine();
            if (username.isEmpty() || !username.matches("[A-Za-z0-9]+") || username.equalsIgnoreCase("you")) {
                sendMessage("Invalid username. Only alphanumeric characters are allowed.");
                throw new IOException("Invalid username");
            }
            System.out.println("Registering user: " + username);
            String pwd = in.readLine();
            if (!AuthManager.register(username, pwd)) {
                sendMessage("Registration failed. Disconnecting.");
                throw new IOException("Reg failed");
            }
        } else {
            sendMessage("Invalid option. Disconnecting.");
            throw new IOException("Invalid option");
        }
        if (LobbyServer.active_users.contains(username)) {      // Check if user is already logged in somewhere else
            sendMessage("User already logged in. Disconnecting.");
            throw new IOException("User already logged in somewhere");
        }
        LobbyServer.printMessage("User " + username + " logged in.");
        sendMessage("Authenticated as " + username);
        if (choice.equals("1") || choice.equals("3")) {
            token_uuid = LobbyServer.generateToken(username);
            sendMessage("Your token: " + token_uuid);
        } else {
            sendMessage("If needed again, your token: " + token_uuid);
        }
        if (!LobbyServer.addActiveUser(username)) {
            sendMessage("User already logged in somewhere else.");
            throw new IOException("User already logged in somewhere else");
        }
    }

    private void change_state(ClientState state) throws IOException {
        sendMessage(":change_state " + state);
        client_state = state;
    }

    private void menu() throws IOException {
        sendMessage(":menu");
    }

    private void sendHistory(String roomName){
        List input = LobbyServer.getMessages(roomName);
        String outputAll = "";
        if(!currentRoom.isAi()) {
            for (var message : input) {
                String msg = message.toString();
                String output = msg.replaceAll("User talking to the other users: (\\w+), (.+),?", "[$1] $2");

                outputAll += output + "\n";
            }
        }else{
            for (var message : input) {
                String msg = message.toString();
                String output = msg.replaceAll("\\{\\s*\"role\"\\s*:\\s*\"(.*?)\",\\s*\"content\"\\s*:\\s*\"(.*?)\"\\s*\\}", "$1 $2");
                String role = output.replaceAll("^\\s*(\\w+)\\s+(.+)", "$1");
                String res = output.replaceAll("^\\s*(\\w+)\\s+(.+)", "$2");
                System.out.println(role);
                if(!role.trim().equals("system")) {
                    if(role.trim().equals("user")) {
                        res = res.replaceAll("User talking to the other users: (\\w+), (.+),?", "[$1] $2");
                        res = res.replaceAll("^\\s*User asking the question to the ai: (\\w+), Message: (.+)", "[$1] ai: $2");
                    }else if(role.trim().equals("assistant")){
                        String bot = "[Bot] ";
                        res = bot + res;
                    }
                    System.out.println(res); 
                    outputAll += res;

                }
            }
        }
        if(outputAll.length() > 0) {
            sendMessage("Chat History: \n");
            sendMessage(outputAll);
        }
    }
    private boolean joinRoom() throws IOException {
        System.out.println("Rooms: " + LobbyServer.rooms.keySet());
        sendMessage("\n--- Available rooms ---");
        if (LobbyServer.rooms.keySet().size() == 0) {
            sendMessage(":no_rooms");
        } else {
            String rooms_list = "";
            for (var i : LobbyServer.rooms.keySet()) {
                rooms_list += "\n  -" + i;
            }
            sendMessage(rooms_list);
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
        // atualiza o estado do cliente
        change_state(ClientState.ROOM);
        sendMessage("Joined room " + roomName);
        sendMessage(":room_help");
        LobbyServer.printMessage("User " + username + " joined room " + roomName);
        return true;
    }

    private void createRoom() throws IOException {
        sendMessage("Enter a Room Name: ");
        String roomName = in.readLine();
        System.out.println("Room Name: " + roomName);

        if (LobbyServer.rooms.containsKey(roomName)) {
            sendMessage("Room already exists.");
            return;
        }
        if (roomName.equals("")) {
            sendMessage("Insert a name please.");
            return;
        }
        if (!roomName.matches("[A-Za-z0-9_]+")) {
            sendMessage("Invalid room name. Only alphanumeric characters and underscores are allowed.");
            return;
        }


        while (true) {
            sendMessage("Do you want an AI bot to be present in the chat? \nAnswer yes or no ");
            String ai = in.readLine();
            if (ai.trim().equals("yes")) {
                Room room = new Room(roomName,true);
                if (!LobbyServer.createRoom(roomName, room)){
                    sendMessage("Room with that name has been created meanwhile.");
                    return;
                }
                LobbyServer.printMessage("Room " + roomName + " created by " + username);
                sendMessage("Please enter a system message to configure how the AI should behave in this chat (e.g., tone, expertise level, personality, goals):");
                String behaviour = in.readLine();
                String userMessage = "You are a chat bot assistant that should always have the following behaviour when answering questions: %s"
                        .formatted(behaviour);
                String prompt = buildOllamaMessage("system",userMessage);

                // Append this message to the room's chat history
                LobbyServer.addPrompt(roomName, prompt);
                sendMessage("Room '" + roomName + "' created.");
                return;
            }
            else if (ai.trim().equals("no")) {
                Room room = new Room(roomName,false);
                if (!LobbyServer.createRoom(roomName, room)){
                    sendMessage("Room with that name has been created meanwhile.");
                    return;
                }
                LobbyServer.printMessage("Room " + roomName + " created by " + username);
                return;
            }else{
                sendMessage("Invalid input. Please write either yes or no.");
            }
        }

    }

    private void chatLoop() throws IOException, InterruptedException {
        sendHistory(currentRoom.getName());
        String msg;
        while ((msg = in.readLine()) != null) {
            if (msg.equalsIgnoreCase(":q")) {
                currentRoom.removeUser(username);
                currentRoom = null;
                LobbyServer.updateTokenRoom(token_uuid, null);
                change_state(ClientState.LOBBY);
                return;
            } else if (msg.startsWith(":ai")) {
                if(!currentRoom.isAi()){
                    sendMessage("This is room has no AI assistant");
                    continue;
                }
                String[] parts = msg.split(" ", 2);
                if (parts.length < 2) {
                    sendMessage("Usage: :ai <message>");
                    continue;
                }
                currentRoom.broadcast("[" + username + "]: " + msg);
                String promptContent = "User asking the question to the ai: " + username + ", Message: " + parts[1];

                String userMessage = buildOllamaMessage("user",promptContent);
                // Append this message to the room's chat history
                LobbyServer.addPrompt(currentRoom.getName(), userMessage);
                Room answerRoom = currentRoom;
                new Thread(() -> {
                    String jsonPayload = buildOllamaPayload(currentRoom.getName());
                    String responseBody = null; // blocking
                    try {
                        responseBody = sendOllamaRequest(jsonPayload);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    String extractedContent = extractContentValue(responseBody,answerRoom.getName());

                    System.out.println("Extracted content: " + extractedContent);
                    System.out.println(responseBody);

                    // Respond in room
                    answerRoom.broadcast("\n[Bot]: " + extractedContent + "\n");
                }).start();
            } else if (msg.equalsIgnoreCase(":logout")) {
                cleanup();
            } else if (msg.equalsIgnoreCase(":u")) {
                currentRoom.listUsers(out);
            } else if (msg.startsWith(":m ")) {
                String[] parts = msg.split(" ", 3);
                if (parts.length < 3) {
                    sendMessage("Usage: :m <username> <message>");
                    continue;
                }
                String receiver = parts[1];
                String privateMessage = parts[2];
                currentRoom.broadcast("[" + username + "] (private): " + privateMessage, receiver);
            } else if (msg.equalsIgnoreCase(":h")) {  // This will never be used since client is not sending :h
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
                if(currentRoom.isAi()){
                    msg = "User talking to the other users: " + username + ", " + msg;
                    msg = buildOllamaMessage("user",msg);
                    LobbyServer.addPrompt(currentRoom.getName(), msg);
                }else{
                    msg = "User talking to the other users: " + username + ", " + msg;
                    LobbyServer.addPrompt(currentRoom.getName(), msg);
                }
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

    private void sendMessage(String message) {
        out.println(message);
        out.flush();
    }

    private String buildOllamaMessage(String role,String promptContent) {
        return """
                    {
                      "role": "%s",
                      "content": "%s"
                    }
                """.formatted(role,promptContent);
    }

    private String buildOllamaPayload(String roomName) {
        List<String> msgList = LobbyServer.getMessages(roomName);
        String allMessages = "";
        for (int i=0;i<msgList.size();i++) {

            if(i==msgList.size()-1){
                allMessages += msgList.get(i) + "\n";
            }else{
                allMessages += msgList.get(i) + ",\n";
            }
        }
        System.out.println("All messages: " + allMessages);
        return """
                    {
                      "model": "llama3",
                      "messages": [%s],
                      "stream": false
                    }
                """.formatted(allMessages);
    }

    private String sendOllamaRequest(String jsonPayload) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String extractContentValue(String responseBody, String answerRoom) {
        Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher matcher = pattern.matcher(responseBody);

        if (matcher.find()) {
            String raw = matcher.group(1);
            // Unescape basic sequences
            String assistantMessage = buildOllamaMessage("assistant",raw);
            LobbyServer.addPrompt(answerRoom, assistantMessage);
            return raw.replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }

        return "[content not found]";
    }

}
