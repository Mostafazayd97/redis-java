import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

class ClientHandler implements Runnable {

    private final Socket socket;
    private final String dir;
    private final String dbfilename;
    public static HashMap<String, String> map = new HashMap<>();

    public ClientHandler(Socket socket, String dir, String dbfilename) {
        this.socket = socket;
        this.dir = dir;
        this.dbfilename = dbfilename;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream output = socket.getOutputStream()) {

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

    private void respondAsRESPArray(OutputStream output, String key, String value) throws IOException {
        output.write(String.format("*2\r\n$%d\r\n%s\r\n$%d\r\n%s\r\n",
                key.length(), key, value.length(), value).getBytes());
    }
}