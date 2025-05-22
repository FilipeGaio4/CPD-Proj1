package TCPServer.lobby;

import TCPServer.models.*;
import TCPServer.lobby.LobbyServer; 
import TCPServer.lobby.AuthManager;

import java.io.*;
import java.net.Socket;

public class ClientLobbyHandler implements Runnable {
    private final Socket socket;
    private String username;
    private PrintWriter out;
    private BufferedReader in;
    private Room currentRoom;
    private String token_uuid;
        private enum ClientState {
        UNDEFINED,
        AUTHENTICATION,
        LOBBY,
        ROOM
    }
    private ClientState client_state = ClientState.UNDEFINED;

    public ClientLobbyHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            setupIO();
            login();
            if (client_state == ClientState.ROOM){
                out.println(":resume");
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
                        out.println(":goodbye");
                        return; 
                    default:
                        out.println("Invalid option.");
                }
            }

        } catch (IOException e) {
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
                out.println("Authentication failed. Disconnecting.");
                throw new IOException("Auth failed");
            }
        }
        else if (choice.equals("2")) {          // Token Login
            String token_uuid = in.readLine();
            System.out.println("Token: " + token_uuid);
            Token token = LobbyServer.consumeToken(token_uuid, out);
            username = token.getUsername();
            if (username == null) {
                out.println("Invalid token. Disconnecting.");
                throw new IOException("Invalid token");
            }
            ClientState state = token.getRoom() != null ? ClientState.ROOM : ClientState.LOBBY;
            Room room = LobbyServer.rooms.get(token.getRoom()); // Double checking beacuse im already checking in consume token
            if (room == null) {
                out.println(":deleted_room");
            }
            // Puts the user in the room even if its null
            currentRoom = room;
            change_state(state);
        }
        else {
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
        if (choice.equals("1")){
            token_uuid = LobbyServer.generateToken(username);
            out.println("Your token: " + token_uuid);
            out.flush();
        }
        LobbyServer.addActiveUser(username);
    }

    private void change_state(ClientState state) throws IOException {
        out.println(":change_state " + state);
        out.flush();
        client_state = state;
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
                
            }else {
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
        // out.println("Updated token: " + LobbyServer.getFullToken(token_uuid));
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
            }if(roomName.equals("")){
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
            out.println("Room '" + roomName + "' created.");
            out.flush();
        }
    }

    private void chatLoop() throws IOException {
        String msg;
        while ((msg = in.readLine()) != null) {
            if (msg.equalsIgnoreCase(":q")) {
                currentRoom.removeUser(username);
                currentRoom = null;
                return;
            }
            else if (msg.equalsIgnoreCase(":u")) {
                synchronized (currentRoom) { // TODO : change here to our lock
                    currentRoom.listUsers(out);

                }
            }
            else if (msg.startsWith(":m ")) {
                String[] parts = msg.split(" ", 3);
                if (parts.length < 3) {
                    out.println("Usage: :m <username> <message>");
                    continue;
                }
                String receiver = parts[1];
                String privateMessage = parts[2];
                currentRoom.broadcast("[" + username + "] (private): " + privateMessage, receiver);
            }
            else if (msg.equalsIgnoreCase(":h")) {
                out.println("RULES and Shortcuts:");
                out.println("- ':q' to leave the room.");
                out.println("- ':u' to list users.");
                out.println("- ':m <username> <message>' to send a private message.");
                out.println("- ':h' to see this help.");
            }
            else if (msg.isEmpty()) {
                continue;
            }
            else {
                System.out.println("BROADCAST: " + msg);
                currentRoom.broadcast("[" + username + "]: " + msg);
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
}
