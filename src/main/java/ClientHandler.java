import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    public static HashMap<String, String> map = new HashMap<>();

    // Configuration parameters
    private static String dir = "/tmp/redis-data";
    private static String dbfilename = "rdbfile";

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream output = socket.getOutputStream()) {

            // Load dataset from RDB file on startup
            loadRDB();

            while (true) {
                String request = input.readLine();
                if (request == null) continue;

                if (request.startsWith("*")) { // RESP parsing
                    int numElements = Integer.parseInt(request.substring(1));
                    String[] elements = new String[numElements];

                    for (int i = 0; i < numElements; i++) {
                        String lengthLine = input.readLine();
                        if (!lengthLine.startsWith("$")) {
                            output.write("-ERR Invalid RESP format\r\n".getBytes());
                            output.flush();
                            return;
                        }
                        int length = Integer.parseInt(lengthLine.substring(1));
                        elements[i] = input.readLine();
                        if (elements[i].length() != length) {
                            output.write("-ERR RESP length mismatch\r\n".getBytes());
                            output.flush();
                            return;
                        }
                    }

                    String command = elements[0].toUpperCase();

                    switch (command) {
                        case "CONFIG":
                            if ("GET".equalsIgnoreCase(elements[1])) {
                                handleConfigGet(elements[2], output);
                            } else {
                                output.write("-ERR Unknown CONFIG subcommand\r\n".getBytes());
                            }
                            break;
                        case "SET":
                            handleSet(elements, output);
                            break;
                        case "GET":
                            handleGet(elements, output);
                            break;
                        default:
                            output.write("-ERR Unknown command\r\n".getBytes());
                            break;
                    }
                } else {
                    output.write("-ERR Invalid protocol format\r\n".getBytes());
                }

                output.flush();
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    // Handle CONFIG GET command
    private void handleConfigGet(String param, OutputStream output) throws IOException {
        switch (param) {
            case "dir":
                respondAsRESPArray(output, "dir", dir);
                break;
            case "dbfilename":
                respondAsRESPArray(output, "dbfilename", dbfilename);
                break;
            default:
                output.write("-ERR Unknown configuration parameter\r\n".getBytes());
        }
    }

    // Handle SET command
    private void handleSet(String[] elements, OutputStream output) throws IOException {
        if (elements.length < 3) {
            output.write("-ERR Invalid SET command\r\n".getBytes());
            return;
        }

        String key = elements[1];
        String value = elements[2];
        map.put(key.toLowerCase(), value);

        output.write("+OK\r\n".getBytes());
    }

    // Handle GET command
    private void handleGet(String[] elements, OutputStream output) throws IOException {
        if (elements.length < 2) {
            output.write("-ERR Missing key for GET command\r\n".getBytes());
            return;
        }

        String key = elements[1].toLowerCase();
        String value = map.get(key);

        if (value == null) {
            output.write("$-1\r\n".getBytes());
        } else {
            output.write(String.format("$%d\r\n%s\r\n", value.length(), value).getBytes());
        }
    }

    // Load data from RDB file
    private void loadRDB() {
        File rdbFile = new File(dir, dbfilename);
        if (!rdbFile.exists()) {
            System.out.println("No RDB file found. Starting with an empty dataset.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(rdbFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] kv = line.split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0].toLowerCase(), kv[1]);
                }
            }
            System.out.println("Data loaded from RDB file.");
        } catch (IOException e) {
            System.err.println("Failed to load RDB file: " + e.getMessage());
        }
    }

    // Serialize data to RDB file
    public static void saveRDB() {
        File rdbFile = new File(dir, dbfilename);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rdbFile))) {
            for (HashMap.Entry<String, String> entry : map.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
            System.out.println("Data saved to RDB file.");
        } catch (IOException e) {
            System.err.println("Failed to save RDB file: " + e.getMessage());
        }
    }

    // Respond as RESP array
    private void respondAsRESPArray(OutputStream output, String key, String value) throws IOException {
        output.write(String.format("*2\r\n$%d\r\n%s\r\n$%d\r\n%s\r\n",
                key.length(), key, value.length(), value).getBytes());
    }
}
