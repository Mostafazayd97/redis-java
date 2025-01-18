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
                    String echo = input.readLine();
                    echo = input.readLine();
                    socket.getOutputStream().write(
                            String.format("$%d\r\n%s\r\n", echo.length(), echo).getBytes());
                    socket.getOutputStream().flush();
                } else if (request.startsWith("SET")) {
                    String key = input.readLine();
                    String value = input.readLine();
                    String metCommand = input.readLine();
                    if(!metCommand.isEmpty() && metCommand.equalsIgnoreCase("PX")){
                        Integer time = Integer.parseInt(input.readLine());
                        Timer timer = new Timer();
                        String finalKey = key;
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                map.remove(finalKey);
                                System.out.println(key+ " has been removed " + value);
                                map
                                        .entrySet().stream().forEach(entry -> System.out.println(entry.getKey() + " " + entry.getValue()));

                            }
                        };
                        timer.schedule(task, time);
                    }
                    map.put(key.toLowerCase(), value);
                    map
                            .entrySet().stream().forEach(entry -> System.out.println(entry.getKey() + " " + entry.getValue()));

                    socket.getOutputStream().write(
                            String.format("$%d\r\n%s\r\n", "OK".length(), "OK").getBytes());
                    socket.getOutputStream().flush();
                }  else if (request.startsWith("GET")) {
                    String key = input.readLine();
                    String value = map.get(key.toLowerCase());
                    if (value == null) {
                        socket.getOutputStream().write("$-1\r\n".getBytes()); // Standard Redis response for null
                    } else {
                        socket.getOutputStream().write(
                                String.format("$%d\r\n%s\r\n", value.length(), value).getBytes());
                    }
                    socket.getOutputStream().flush();
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

}