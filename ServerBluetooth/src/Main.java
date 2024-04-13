import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class Main {
    int[] name = new int[4];

    /*

Create Server
Start Listening
Accept Incoming Requests
Confirm Request is bluetooth mouse
else
Throw it out
Once connected, call close()
*/

    /*

Resources:
Bluetooth Socket*/
    public static void main(String[] args) {
        try {// Create a UUID for SPP (Serial Port Profile)
            UUID uuid = new UUID("00001101-0000-1000-8000-00805F9B34FB", false);

            // Create a server socket
            StreamConnectionNotifier notifier = (StreamConnectionNotifier) Connector
                    .open("btspp://localhost:" + uuid + ";name=SampleServer");

            System.out.println("Server started. Waiting for client connection...");

            // Accept client connection
            StreamConnection connection = notifier.acceptAndOpen();

            System.out.println("Client connected.");

            // Get input and output streams
            InputStream inputStream = connection.openInputStream();
            OutputStream outputStream = connection.openOutputStream();

            // Handle communication
            byte[] buffer = new byte[1024];
            int bytes;
            while ((bytes = inputStream.read(buffer)) != -1) {
                String receivedMessage = new String(buffer, 0, bytes);
                System.out.println("Received: " + receivedMessage);

                // Process received message here

                // Example: Echo back the message to the client
                outputStream.write(receivedMessage.getBytes());
            }

            // Close streams and connection
            inputStream.close();
            outputStream.close();
            connection.close();
            notifier.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}