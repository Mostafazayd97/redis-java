
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        try {
            serverSocket = new ServerSocket(port);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            clientSocket = serverSocket.accept();
            while (true) {
                byte buffer[] = new byte[1024];
                int bytesRead = clientSocket.getInputStream().read(buffer);
                String request = new String(buffer, 0, bytesRead);

                if (request.contains("PING")) {
                    OutputStream outputStream = clientSocket.getOutputStream();
                    String reponse = "+PONG\r\n";
                    outputStream.write(reponse.getBytes());
                    outputStream.flush();
                    break;
                }

            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
