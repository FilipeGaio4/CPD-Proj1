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

    public ClientLobbyHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            setupIO();
            login();
            while (true) {
                menu();
                String choice = in.readLine();
                System.out.println(choice);
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
                        out.println("Goodbye!");
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

    private void login() throws IOException {
        //out.println("Welcome! Please log in.");
        //out.print("Username: "); out.flush();
        username = in.readLine();
        System.out.println("Username: " + username);
        //out.print("Password: "); out.flush();
        String pwd = in.readLine();

        if (LobbyServer.active_users.contains(username)) {
            out.println("User already logged in. Disconnecting.");
            throw new IOException("User already logged in somewhere");
        }
        if (!AuthManager.authenticate(username, pwd)) {
            out.println("Authentication failed. Disconnecting.");
            throw new IOException("Auth failed");
        }
        LobbyServer.printMessage("User " + username + " logged in.");
        out.println("Authenticated as " + username);
        token_uuid = LobbyServer.generateToken(username);
        out.println("Your token: " + token_uuid);
        out.flush();
        LobbyServer.addActiveUser(username);
    }

    private void menu() throws IOException {
        System.out.println("Rooms: " + LobbyServer.rooms.keySet());
        out.println("\n--- Available rooms ---");
        synchronized (LobbyServer.rooms) { // TODO : change here to our lock
            //out.println(LobbyServer.rooms.keySet());
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
        out.println(":menu");
        out.flush();
    }


    private boolean joinRoom() throws IOException {
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
