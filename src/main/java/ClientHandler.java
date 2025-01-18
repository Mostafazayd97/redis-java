import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class ClientHandler implements Runnable {

    private final Socket socket;
    public static HashMap<String, String> map = new HashMap<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while (true) {
                String request = input.readLine();
                if (request == null) {
                    continue;
                }

                if (request.startsWith("*")) { // RESP Array
                    int numElements = Integer.parseInt(request.substring(1)); // Number of elements in the array
                    String[] elements = new String[numElements];

                    // Parse RESP array elements
                    for (int i = 0; i < numElements; i++) {
                        String lengthLine = input.readLine(); // e.g., "$10"
                        if (!lengthLine.startsWith("$")) {
                            throw new IOException("Invalid RESP format");
                        }
                        int length = Integer.parseInt(lengthLine.substring(1)); // Parse the length of the string
                        elements[i] = input.readLine(); // Read the actual string
                        if (elements[i].length() != length) {
                            throw new IOException("Mismatched RESP length");
                        }
                    }

                    // Handle the command
                    String command = elements[0].toUpperCase(); // Command name (e.g., SET, GET, etc.)
                    switch (command) {
                        case "SET": {
                            handleSetCommand(elements);
                            break;
                        }
                        case "GET": {
                            handleGetCommand(elements);
                            break;
                        }
                        case "PING": {
                            socket.getOutputStream().write("+PONG\r\n".getBytes());
                            socket.getOutputStream().flush();
                            break;
                        }
                        case "ECHO": {
                            if (elements.length >= 2) {
                                String echoMessage = elements[1];
                                socket.getOutputStream().write(
                                        String.format("$%d\r\n%s\r\n", echoMessage.length(), echoMessage).getBytes());
                                socket.getOutputStream().flush();
                            } else {
                                socket.getOutputStream().write("-ERR Missing argument for ECHO\r\n".getBytes());
                            }
                            break;
                        }
                        default: {
                            socket.getOutputStream().write("-ERR Unknown command\r\n".getBytes());
                            socket.getOutputStream().flush();
                            break;
                        }
                    }
                } else {
                    socket.getOutputStream().write("-ERR Invalid protocol format\r\n".getBytes());
                    socket.getOutputStream().flush();
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private void handleSetCommand(String[] elements) throws IOException {
        if (elements.length < 3) {
            socket.getOutputStream().write("-ERR Invalid SET command\r\n".getBytes());
            return;
        }

        String key = elements[1];
        String value = elements[2];

        if (elements.length >= 5 && "PX".equalsIgnoreCase(elements[3])) {
            int ttl = Integer.parseInt(elements[4]); // Time-to-live in milliseconds
            Timer timer = new Timer();
            String finalKey = key.toLowerCase();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    map.remove(finalKey);
                    System.out.println("Key expired: " + finalKey);
                }
            };
            timer.schedule(task, ttl);
        }

        map.put(key.toLowerCase(), value); // Store the key-value pair
        socket.getOutputStream().write("+OK\r\n".getBytes()); // RESP OK response
        socket.getOutputStream().flush();
    }

    private void handleGetCommand(String[] elements) throws IOException {
        if (elements.length < 2) {
            socket.getOutputStream().write("-ERR Missing key for GET command\r\n".getBytes());
            return;
        }

        String key = elements[1].toLowerCase();
        String value = map.get(key);

        if (value == null) {
            socket.getOutputStream().write("$-1\r\n".getBytes()); // RESP Null Bulk String
        } else {
            socket.getOutputStream().write(
                    String.format("$%d\r\n%s\r\n", value.length(), value).getBytes());
        }
        socket.getOutputStream().flush();
    }
}
