package Assignment2;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static String SERVER_ADDRESS = "localhost";
    private static int SERVER_PORT = 12345;
    private static String username = "";
    private static String password = "";
    private static final String TRUSTSTORE_FILE = "Assignment2/client.truststore";  // Truststore file containing server's public certificate
    private static final String TRUSTSTORE_PASSWORD = "123456";
    private static SSLSocket socket;
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
            System.out.println("Available rooms:");

            System.out.println(in.readLine());


            System.out.println("\n1 - Join a room");
            System.out.println("2 - Create a new room");
            System.out.println("3 - Quit");
            System.out.print("Choice: ");

            String choice = scanner.nextLine();
            out.println(choice);
            out.flush();
            if (choice.equals("3")) {
                return;
            } else {

                String roomName = in.readLine();
                System.out.println(roomName);
                choice = scanner.nextLine();
                out.println(choice);
                out.flush();

                System.out.println(in.readLine());

            }
        }


    }
}