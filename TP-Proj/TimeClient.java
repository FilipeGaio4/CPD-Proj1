import java.net.*;
import java.util.Scanner;
import java.io.*;
 
/**
 * This program demonstrates a simple TCP/IP socket client.
 *
 * @author www.codejava.net
 */
public class TimeClient {
 
    public static int backoff = 1000;
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 2) return;
 
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
 
        try (Socket socket = new Socket(hostname, port)) {
 
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Scanner inputCmd = new Scanner(System.in);
            String cmd = inputCmd.nextLine();
            if (cmd.equals("QUIT")) {
                String quitMessage = "shutdown now";
                writer.println(quitMessage);
                writer.flush();
                inputCmd.close();
                socket.close();
                System.exit(1);
            }
            String[] cmdParts = cmd.split(" ");
            if (cmdParts.length > 1) {
                if (cmdParts[0].equals("put")) {
                    // Process the put command
                    System.out.println("Received put command with value: " + cmdParts[1]);
                    writer.println(cmd);
                    writer.flush();
                } else if (cmdParts[0].equals("get")) {
                    // Process the get command
                    System.out.println("Received get command with value: " + cmdParts[1]);
                    writer.println(cmd);
                    writer.flush();
                    System.out.println(reader.readLine());
                }
            } else {
                System.out.println("Invalid command. Use 'put <value>' or 'get <value>'");
            }


            // writer.println((hostname + ":" + port).toString());
            // writer.flush();
            // writer.println("new Date()?".toString());
            // writer.flush();
            // writer.println("Hello from client".toString());
            // writer.flush();
            // writer.println(java.time.Instant.now().toString());
            // writer.flush();

            InputStream input = socket.getInputStream();
            BufferedReader readerFinal = new BufferedReader(new InputStreamReader(input));
 
            String time = readerFinal.readLine();
 
            System.out.println(time);
 
 
        } catch (UnknownHostException ex) {
 
            System.out.println("Server not found: " + ex.getMessage());
 
        } catch (IOException ex) {
 
            System.out.println("I/O error: " + ex.getMessage());
            Thread.sleep(backoff);
            backoff *= 2;
            System.out.println("Retrying connection...");
            main(args);
        }
    }
}