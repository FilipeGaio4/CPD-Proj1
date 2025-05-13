package UDP;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;

public class server {
  public static void main(String[] args) throws IOException, InterruptedException {
    DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt("5001"));
    System.out.println("Server Started. Listening for Clients on port 5001" + "...");

    byte[] receiveData = new byte[1024]; // e se for maior que 1024?
    DatagramPacket receivePacket;
    while (true) {
        // Server waiting for clients message
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        // Get the client's IP address and port
        InetAddress IPAddress = receivePacket.getAddress();
        int port = receivePacket.getPort();
        // Convert Byte Data to String
        String clientMessage = new String(receivePacket.getData(),0,receivePacket.getLength());

        if (clientMessage.equals("shutdown")) {
          System.out.println("Server Stopped.");
          serverSocket.close();
          System.exit(1);
        }

        String[] cmdParts = clientMessage.split(" ");
        if (cmdParts.length > 1) {
            if (cmdParts[0].equals("put")) {
                // Process the put command
                System.out.println("Received put command with value: " + cmdParts[1]);
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                System.out.println("[" + timestamp.toString() + " ,IP: " + IPAddress + " ,Port: " + port +"]  " + clientMessage);
            } else if (cmdParts[0].equals("get")) {
                // Process the get command
                System.out.println("Received get command with value: " + clientMessage);
                String responseMessage = "Recebi a mensagem: " + clientMessage;
                // Thread.sleep(4000);  // Com este delay o client fecha porque dps de 3 segundos sem resposta fecha
                serverSocket.send(new DatagramPacket(responseMessage.getBytes(), responseMessage.length(), IPAddress, port));
            }
        } else {
            System.out.println("Invalid command. Use 'put <value>' or 'get <value>'");
        }

    }
  }
}