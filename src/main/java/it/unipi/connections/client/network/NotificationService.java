package it.unipi.connections.client.network;

import it.unipi.connections.client.ui.ClientView;
import it.unipi.connections.common.notifications.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Listens for asynchronous UDP notifications and dispatches them to the UI layer. Leverages
 * polymorphic DTOs for type-safe processing.
 */
public class NotificationService extends Thread implements AutoCloseable {

    private static final int BUFFER_SIZE = 4096;

    private final DatagramSocket socket;
    private final ClientView view;
    private final Gson gson = new Gson();

    public NotificationService(int port, ClientView view) throws SocketException {
        DatagramSocket s;
        try {
            s = new DatagramSocket(port);
        } catch (SocketException e) {
            // Fallback to any available port if requested is busy
            s = new DatagramSocket(0);
        }
        this.socket = s;
        this.view = view;
        setDaemon(true);
        setName("NotificationService-Thread");
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    /**
     * Main execution loop for the notification service. Continuously listens for incoming UDP
     * packets, processes them, and triggers the appropriate UI callbacks.
     */
    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (!isInterrupted() && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength());
                processNotification(json);

            } catch (IOException e) {
                if (!socket.isClosed()) {
                    view.showError("Notification service error: " + e.getMessage());
                }
                break;
            } catch (Exception e) {
                view.showError("Unexpected error in notification service: " + e.getMessage());
            }
        }
    }

    /**
     * Identifies the notification type and triggers the specific view callback. Uses the mandatory
     * 'type' field from the Notification abstract class.
     */
    private void processNotification(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has(Notification.FIELD_TYPE))
                return;

            String typeId = obj.get(Notification.FIELD_TYPE).getAsString();
            NotificationType type = NotificationType.fromId(typeId);

            if (type != null) {
                Notification n = gson.fromJson(json, type.getNotificationClass());
                view.render(n);
            } else {
                view.showError("Received unknown notification type: " + typeId);
            }

        } catch (Exception e) {
            view.showError("Failed to process notification: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        interrupt();
        socket.close();
    }
}
