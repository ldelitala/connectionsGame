package com.connections.client.network;

import com.connections.client.ui.ClientView;
import com.connections.common.notifications.*;
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
        this.socket = new DatagramSocket(port);
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

            String type = obj.get(Notification.FIELD_TYPE).getAsString();

            // Deserialization must target the specific subclass to populate all fields
            switch (type) {
                case NewGameNotification.TYPE_ID:
                    view.onNewPuzzle(gson.fromJson(json, NewGameNotification.class));
                    break;
                case GameEndNotification.TYPE_ID:
                    view.onPuzzleEnd(gson.fromJson(json, GameEndNotification.class));
                    break;
                case PlayerGameSummaryNotification.TYPE_ID:
                    view.onPlayerSummary(gson.fromJson(json, PlayerGameSummaryNotification.class));
                    break;
                default:
                    // Unknown notification type
                    break;
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
