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
                    String echo = input.readLine();

                    echo = input.readLine();

                    socket.getOutputStream().write(

                            String.format("$%d\r\n%s\r\n", echo.length(), echo).getBytes());

                    socket.getOutputStream().flush();
                } else if (request.startsWith("SET")) {
                    String key = input.readLine();

                    key = input.readLine();

                    String value = input.readLine();

                    value = input.readLine();

                    map.put(key, value);

                    socket.getOutputStream().write(

                            String.format("$%d\r\n%s\r\n", "OK".length(), "OK").getBytes());

                    socket.getOutputStream().flush();
                } else if (request.startsWith("GET")) {
                    String key = input.readLine();

                    key = input.readLine();

                    String value = map.get(key);

                    socket.getOutputStream().write(

                            String.format("$%d\r\n%s\r\n", value.length(), value).getBytes());

                    socket.getOutputStream().flush();
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

}