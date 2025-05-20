package TCPServer.models;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Room {
    private final String name;
    private final Map<String, PrintWriter> users = new HashMap<>(); // username -> output stream

    public Room(String name) {
        this.name = name;
    }

    public synchronized void addUser(String username, PrintWriter out) { // TODO : change here to our lock
        users.put(username, out);
        broadcast("[" + username + "] joined the room.");
    }

    public synchronized void removeUser(String username) { // TODO : change here to our lock
        users.remove(username);
        broadcast("[" + username + "] left the chat...F's in the chat.");
    }

    public synchronized void listUsers(PrintWriter out) { // TODO : change here to our lock
        out.println("Users in the room:");
        for (String user : users.keySet()) {
            out.println("- " + user);
        }
    }

    public synchronized void broadcast(String message) { // TODO : change here to our lock
        for (PrintWriter out : users.values()) {
            out.println(message); // TODO : Test if one user is slow if no one receives it
        }
    }

    public synchronized void broadcast(String message, String receiver) { // TODO : change here to our lock
        PrintWriter out = users.get(receiver);
        if (out != null) {
            out.println(message);
        }
    }

    public String getName() {
        return name;
    }

    public boolean hasUser(String username) {
        return users.containsKey(username);
    }
}
