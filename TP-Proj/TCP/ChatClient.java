import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            System.out.println("Connected to the chat server.");
            new Thread(new ReadHandler(socket)).start();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // Escreve no socket
            Scanner scanner = new Scanner(System.in); // Vai dando scan ao que escrevemos no Sys.in

            // Authentication
            out.println(scanner.nextLine());
            // TODO: Já conseguimos ver as mensagens antes de nos autenticarmos. ele dá broadcast para todos e ocmo estamos no socket. Mas nao conseguimos enviar
            
            // Chat loop
            String message;
            while (true) {
                if (socket.isClosed()) { // TODO: Isto funciona mas só quando ele manda uma mensagem é que ele volta a fazer o ceck. arranjar forma de melhorar
                    System.out.println("This socket is closed.");
                    scanner.close();
                    return;
                }
                message = scanner.nextLine();
                if (message.equalsIgnoreCase("QUIT")) {
                    out.println("QUIT");
                    scanner.close();
                    break;
                }
                out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static class ReadHandler implements Runnable {
        private Socket socket;

        public ReadHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
            }
        }
    }
}