package TCPServer.client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
    private static String SERVER_ADDRESS = "localhost";
    private static int SERVER_PORT = 12345;
    private static String username = "";
    private static String password = "";
    private static final String TRUSTSTORE_FILE = "TCPServer/client/client.truststore";  // Truststore file containing server's public certificate
    private static final String TRUSTSTORE_PASSWORD = "123456";
    private static Map<String, Integer> rooms = new HashMap<>(); // nome da sala -> porta


    public static void main(String[] args) {
        try {
            // Load truststore
            System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE);
            System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);

            // Create SSL socket
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to the server securely");

            // Handle communication with the server (same as before)
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            System.out.println("Welcome to the chat system! Please log in.");
            System.out.println("Enter Username: ");
            username = scanner.nextLine();
            out.println(username);
            out.flush();

            System.out.println("Enter Password: ");
            password = scanner.nextLine();
            out.println(password);
            out.flush();

            String res = in.readLine();
            System.out.println(res);

            if (!res.contains("failed")) {
                handleMenu(out, in, scanner);
            }

        } catch (IOException e) {
            System.out.printf("IOException: %s\n", e.getMessage());
        }

    }

    private static void handleMenu(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        while (true) {
            System.out.println("\n--- MENU ---");
            String answer = in.readLine();
            if (!answer.equals("[]")) {
                for (var i: answer.replaceAll("[\\[\\],]", "").split(" ")) {
                    System.out.println("- "+ i);
                }
            }else{
                System.out.println("(No rooms available yet)");
            }
            System.out.println("1 - Join a room");
            System.out.println("2 - Create a new room");
            System.out.println("3 - Quit");
            System.out.print("Choice: ");
            String choice = scanner.nextLine();
            out.println(choice);

            switch (choice) {
                case "1":
                    if (handleRoom(out, in, scanner)) chat(out, in, scanner);
                    break;
                case "2":
                    handleRoom(out, in, scanner);
                    break;
                case "3":
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static boolean handleRoom(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        System.out.print("Enter Room name: ");
        String roomName = scanner.nextLine();
        out.println(roomName);
        String response = in.readLine();
        System.out.println(response);
        return !response.contains("not exist");
    }


    private static void chat(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        System.out.println("Entered room: " + in.readLine());
        System.out.println("Type ':q' to leave and ':h' for help.");

        Thread listenerThread = new Thread(() -> {
            try {
                String incomingMessage;
                while ((incomingMessage = in.readLine()) != null) {
                    System.out.println(incomingMessage);
                }
            } catch (IOException e) {
                System.out.println("Connection closed or error occurred.");
            }
        });
        listenerThread.start();

        String msg;
        while (true) {
            msg = scanner.nextLine();
            if (msg.equalsIgnoreCase(":q")) {
                out.println(msg);
                System.out.println("Left the room.");
                break;
            } else if (msg.equalsIgnoreCase(":h")) {
                displayHelp();
            } else if (msg.equalsIgnoreCase(":u")) {
                out.println(msg);
            } else if (msg.startsWith(":m ")) {
                out.println(msg);
            } else if (!msg.isEmpty()) {
                out.println(msg);
            }
        }

        try {
            listenerThread.interrupt();
        } catch (Exception e) {
            System.out.println("Error stopping listener thread: " + e.getMessage());
        }

}

    private static void displayHelp() {
        System.out.println("RULES and Shortcuts:");
        System.out.println("- ':q' to leave the room.");
        System.out.println("- ':u' to list users.");
        System.out.println("- ':m <username> <message>' to send a private message.");
        System.out.println("- ':h' to see this help.");
    }

}