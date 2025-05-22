package TCPServer.models;

import TCPServer.lobby.LobbyServer;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Room {
    private final String name;
    private final Map<String, PrintWriter> users = new HashMap<>(); // username -> output stream
    private static final ReentrantLock lock = new ReentrantLock();

    public Room(String name) {
        this.name = name;
    }

    public void addUser(String username, PrintWriter out) { // TODO : change here to our lock
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


    public void removeUser(String username) { // TODO : change here to our lock
        lock.lock();
        try {
            users.remove(username);
            broadcast("[" + username + "] left the chat...F's in the chat.");
    
        } finally {
            lock.unlock();
        }
    }

    public void listUsers(PrintWriter out) { // TODO : change here to our lock
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

    public void broadcast(String message) { // TODO : change here to our lock
        lock.lock();
        try {
            for (PrintWriter out : users.values()) {
                out.println(message); // TODO : Test if one user is slow if no one receives it
            }
        } finally {
            lock.unlock();
        }
    }

    public void broadcast(String message, String receiver) { // TODO : change here to our lock
        PrintWriter out = users.get(receiver);
        if (out != null) {
            out.println(message); // TODO : Test if one user is slow if no one receives it
        }
    }

    public String getName() {
        return name;
    }

    public boolean hasUser(String username) {
        return users.containsKey(username);
    }
}
