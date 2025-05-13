import java.io.*;
import java.net.*;
import java.util.Date;
 
/**
 * This program demonstrates a simple TCP/IP socket server.
 *
 * @author www.codejava.net
 */
public class TimeServer {
 
    public static void main(String[] args) {
        if (args.length < 1) return;
 
        int port = Integer.parseInt(args[0]);
 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
 
            System.out.println("Main server is listening on port " + port);
 
            while (true) {
                Socket socket = serverSocket.accept();
 
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
 
                String time = reader.readLine();
                String[] parts = time.split(" ");
                System.out.println(parts);
                if (parts.length > 1) {
                    // Process the put command
                    if (parts[0].equals("put")) {
                        System.out.println("Received put command with value: " + parts[1]);
                    } 
                    else if (parts[0].equals("get")) {
                        // Process the get command
                        System.out.println("Received get command with value: " + parts[1]);
                        writer.println("Received put command with value: " + time);
                    }
                    else if (parts[0].equals("shutdown")) {
                        System.out.println("Server Stopped.");
                        socket.close();
                        serverSocket.close();
                        System.exit(1);
                    } 
                }
                while (time != null) {
                    System.out.println("Received: " + time);
                    time = reader.readLine();
                }
                
                System.out.println("New client connected: "+ time);
 
                OutputStream output = socket.getOutputStream();
                PrintWriter writerFinal = new PrintWriter(output, true);
 
                writerFinal.println(new Date().toString());
            }
 
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}