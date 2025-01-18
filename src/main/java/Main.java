
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    private static String dir = "/tmp/redis-data"; // Default directory
    private static String dbfilename = "rdbfile"; // Default filename

    public static void main(String[] args) {
        // Parse command-line arguments
        for (int i = 0; i < args.length; i++) {
            if ("--dir".equals(args[i]) && i + 1 < args.length) {
                dir = args[i + 1];
                i++;
            } else if ("--dbfilename".equals(args[i]) && i + 1 < args.length) {
                dbfilename = args[i + 1];
                i++;
            }
        }

        System.out.println("Starting Redis server with the following configuration:");
        System.out.println("dir: " + dir);
        System.out.println("dbfilename: " + dbfilename);

        try (ServerSocket serverSocket = new ServerSocket(6379)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, dir, dbfilename)).start();
            }
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
        }
    }
}
