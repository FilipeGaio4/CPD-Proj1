package UDP;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class client {

  public static void main(String[] args) throws IOException, InterruptedException {
    DatagramPacket sendPacket;
    byte[] sendData;

    DatagramSocket clientSocket = new DatagramSocket();

    clientSocket.setSoTimeout(1000);
    Scanner input = new Scanner(System.in);
    while (true) {
      String cmd = input.nextLine();
      if (cmd.equals("QUIT")) {
        String quitMessage = "shutdown";
        sendData = quitMessage.getBytes();
        sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), 5001);
        clientSocket.send(sendPacket);
        clientSocket.close();
        input.close();
        System.exit(1);
      }
      String[] cmdParts = cmd.split(" ");
      if (cmdParts.length > 1) {
        if (cmdParts[0].equals("put")){
            for (int i = 1; i < cmdParts.length; i++) {
                String putMessage = "put " + cmdParts[i];
                sendData = putMessage.getBytes();
                sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), 5001);
                clientSocket.send(sendPacket);
                Thread.sleep(1000);
            }
        }
        else if (cmdParts[0].equals("get")) {
            sendData = cmd.getBytes();
            sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), 5001);
            clientSocket.send(sendPacket);  
            
            clientSocket.setSoTimeout(3000);  // 3 segundos
            byte[] responseBuffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
            try {
                clientSocket.receive(response);
                String responseMsg = new String(response.getData(), 0, response.getLength());
                System.out.println("Response: " + responseMsg);
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout waiting for response from server.");
            }
        }
        } else {
            System.out.println("Invalid command. Use 'put <value>' or 'get <value>'");
            System.out.println("Client Socket name: "+clientSocket.toString());
            System.out.println("Client Socket InetAddress: "+clientSocket.getInetAddress());
            System.out.println("Client Socket Port: "+clientSocket.getPort());
            System.out.println(clientSocket.getLocalAddress());
            System.out.println(clientSocket.getLocalPort());
            System.out.println(clientSocket.getRemoteSocketAddress());
            System.out.println(clientSocket.getRemoteSocketAddress());
        }

    //   sendData = cmd.getBytes();
    //   sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), 5001);
    //   clientSocket.send(sendPacket);
    }
  }
}