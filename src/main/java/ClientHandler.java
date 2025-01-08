import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // Step 1: Get the input and output streams of the client socket
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            // Step 2: Handle client communication in a loop
            byte[] buffer = new byte[1024];
            while (true) {
                // Read data from the client
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    // If the client disconnects, break the loop
                    System.out.println("Client disconnected.");
                    break;
                }

                // Convert the client input to a string
                String request = new String(buffer, 0, bytesRead).trim();
                System.out.println("Received from client: " + request);

                // Step 3: Respond to the client
                String response = "+PONG\r\n"; // RESP simple string
                outputStream.write(response.getBytes());
                outputStream.flush(); // Ensure the data is sent
                System.out.println("Sent response: " + response.trim());
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            // Step 4: Close the streams and socket
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException while closing resources: " + e.getMessage());
            }
        }
    }
}