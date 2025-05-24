package TCPServer.lobby;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.io.*;


public class AuthManager {
    private static final String USER_FILE = "TCPServer/data/users.txt";
    private static final Map<String, String[]> credentials = new HashMap<>();

    public static void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    credentials.put(parts[0], new String[]{parts[1], parts[2]});
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user file: " + e.getMessage());
        }
    }

    public static boolean authenticate(String username, String password) {
        if (!credentials.containsKey(username)) return false;
        String[] data = credentials.get(username);
        String salt = data[0];
        String hash = data[1];
        String inputHash = hashPassword(password, salt);
        return inputHash.equals(hash);
    }

    public static boolean register(String username, String password) {
        if (credentials.containsKey(username)) return false;
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        credentials.put(username, new String[]{salt, hash});
        saveUser(username, salt, hash);
        return true;
    }

    private static void saveUser(String username, String salt, String hash) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USER_FILE, true))) {
            writer.println(username + ":" + salt + ":" + hash);
        } catch (IOException e) {
            System.out.println("Error saving user: " + e.getMessage());
        }
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashed = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // public static String hashUsers() {
    //     StringBuilder sb = new StringBuilder();
    //     for (Map.Entry<String, String[]> entry : credentials.entrySet()) {
    //         String username = entry.getKey();
    //         String[] data = entry.getValue();
    //         String salt = data[0];
    //         String hash = data[1];
    //         sb.append(username).append(":").append(salt).append(":").append(hash).append("\n");
    //     }
    //     return sb.toString();
    // }
}
