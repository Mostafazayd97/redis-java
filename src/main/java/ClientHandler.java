import java.io.BufferedReader;

import java.io.IOException;

import java.io.InputStreamReader;

import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;

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

                            String.format("$%d\r\n%s\r\n", message.length(), message)

                                    .getBytes());

                }

            }

        } catch (IOException e) {

            System.out.println("IOException: " + e.getMessage());

        }

    }

}