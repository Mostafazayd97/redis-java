import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    public static HashMap<String, String> map = new HashMap<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override

    public void run() {
        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            while (true) {
                String request = input.readLine();
                if (request == null) {
                    continue;
                }
                if ("PING".equals(request)) {
                    socket.getOutputStream().write("+PONG\r\n".getBytes());
                } else if ("ECHO".equalsIgnoreCase(request)) {
                    input.readLine();
                    String message = input.readLine();
                    socket.getOutputStream().write(
                            String.format("\r\n%s\r\n", message)
                                    .getBytes());
                }  else if (request.startsWith("*")) {
                    // Handle RESP2-formatted input
                    try {
                        String[] parts = parseRESP2(input);
                        String command = parts[0];
                        String key = parts[1];
                        String value = parts[2];

                        if ("SET".equals(command)) {
                            map.put(key, value);
                            System.out.println(map.get(key) + " sssssssssssssssss");
                            socket.getOutputStream().write("+OK\r\n".getBytes());
                        } else {
                            socket.getOutputStream().write("-ERR unknown command\r\n".getBytes());
                        }
                    } catch (Exception e) {
                        socket.getOutputStream().write("-ERR invalid RESP2 format\r\n".getBytes());
                    }
                } else if (request.startsWith("GET")) {
                    String key = request.split(" ")[1];
                    String value = map.get(key);
                    if (value == null) {
                        socket.getOutputStream().write("$-1\r\n".getBytes());
                    } else {
                        socket.getOutputStream().write(
                                String.format("$%d\r\n%s\r\n", value.length(), value)
                                        .getBytes());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
    private static String[] parseRESP2(BufferedReader input) throws IOException {
        String line = input.readLine(); // Read the first line (e.g., "*3")
        if (line == null || !line.startsWith("*")) {
            throw new IllegalArgumentException("Invalid RESP2 format: expected array");
        }

        int arrayLength = Integer.parseInt(line.substring(1)); // Get the number of elements
        if (arrayLength != 3) {
            throw new IllegalArgumentException("Invalid RESP2 format: expected 3 elements");
        }

        // Read the command (e.g., "$3\r\nSET\r\n")
        input.readLine(); // Read the length of the command (e.g., "$3")
        String command = input.readLine(); // Read the command (e.g., "SET")

        // Read the key (e.g., "$6\r\norange\r\n")
        input.readLine(); // Read the length of the key (e.g., "$6")
        String key = input.readLine(); // Read the key (e.g., "orange")

        // Read the value (e.g., "$6\r\nbanana\r\n")
        input.readLine(); // Read the length of the value (e.g., "$6")
        String value = input.readLine(); // Read the value (e.g., "banana")

        return new String[]{command, key, value};
    }
}