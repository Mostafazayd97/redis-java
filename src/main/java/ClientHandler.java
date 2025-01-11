import java.io.BufferedReader;

import java.io.IOException;

import java.io.InputStreamReader;

import java.net.Socket;
import java.util.HashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    public static HashMap<String, String> map = new HashMap<>();

    public ClientHandler(Socket socket) { this.socket = socket; }

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
                }
            else if (request.startsWith("SET")) {
                    System.out.println(request + " request");
                   String[] parts = request.split(" ");
                    String key = parts[0];
                    String value = parts[1];
                    map.put(key, value);
                    System.out.println(map.get(key)+  " sssssssssssssssss");
                    socket.getOutputStream().write("+OK\r\n".getBytes());
                }

            else if (request.startsWith("GET")) {
                    String key = request.split(" ")[1];
                    String value = map.get(key);
                    if (value == null) {
                        socket.getOutputStream().write("$-1\r\n".getBytes());
                    } else {
                        socket.getOutputStream().write(
                                String.format("n%s\r\n", value)
                                        .getBytes());
                    }
                } else if (request.startsWith("*")) {
                    String[] parts = request.split("\r\n");
                    String key = parts[1];
                    String value = parts[2];
                    map.put(key, value);
                    System.out.println(map.get(key)+  " sssssssssssssssss");
                    socket.getOutputStream().write("+OK\r\n".getBytes());

                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}