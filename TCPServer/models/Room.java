package TCPServer.models;

import TCPServer.lobby.LobbyServer;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Room {
    private final String name;
    private final boolean ai;
    private final Map<String, PrintWriter> users = new HashMap<>(); // username -> output stream
    private static final ReentrantLock lock = new ReentrantLock();

    public Room(String name, boolean ai) {
        this.name = name;
        this.ai = ai;
    }

    public void addUser(String username, PrintWriter out) {
        lock.lock();
        try {
            users.put(username, out);
            broadcast("[" + username + "] joined the room.");
        } finally {
            lock.unlock();
        }
    }

    public boolean isUserInRoom(String username) {
        lock.lock();
        try {
            return users.containsKey(username);
        } finally {
            lock.unlock();
        }
    }


    public void removeUser(String username) {
        lock.lock();
        try {
            users.remove(username);
            broadcast("[" + username + "] left the chat...F's in the chat.");
    
        } finally {
            lock.unlock();
        }
    }

    public void listUsers(PrintWriter out) { 
        lock.lock();
        try {
            for (var i : users.keySet()) {
                out.println("- " + i);
            }
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    public void broadcast(String message) { 
        lock.lock();
        try {
            for (PrintWriter out : users.values()) {
                out.println(message);
            }
        } finally {
            lock.unlock();
        }
    }

    public void broadcast(String message, String receiver) { 
        PrintWriter out = users.get(receiver);
        if (out != null) {
            out.println(message); 
        }
    }

    public String getName() {
        return name;
    }

    public boolean isAi() {
        return ai;
    }

    public boolean hasUser(String username) {
        return users.containsKey(username);
    }
}
