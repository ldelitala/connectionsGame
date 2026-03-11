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
 * Listens for asynchronous UDP notifications and dispatches them to the UI layer.
 */
public class NotificationService extends Thread implements AutoCloseable {

    private static final int BUFFER_SIZE = 4096;
    private static final String FIELD_TYPE = "type";

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
            }
        }
    }

    /**
     * Identifies the notification type and triggers the specific view callback.
     */
    private void processNotification(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has(FIELD_TYPE)) return;

            String type = obj.get(FIELD_TYPE).getAsString();

            switch (type) {
                case "new_game":
                    view.onNewPuzzle(gson.fromJson(json, NewGameNotification.class));
                    break;
                case "game_end":
                    view.onPuzzleEnd(gson.fromJson(json, GameEndNotification.class));
                    break;
                case "player_summary":
                    view.onPlayerSummary(gson.fromJson(json, PlayerGameSummaryNotification.class));
                    break;
                default:
                    // Unknown notification type, silently ignore or log if necessary
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
