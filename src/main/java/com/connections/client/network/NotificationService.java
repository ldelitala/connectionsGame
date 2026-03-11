package com.connections.client.network;

import com.connections.client.ui.ClientView;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Listens for asynchronous UDP notifications from the server.
 */
public class NotificationService extends Thread implements AutoCloseable {
    private final DatagramSocket socket;
    private final ClientView view;

    /**
     * Initializes the UDP notification service.
     *
     * @param port Preferred UDP port. Use 0 for an ephemeral port.
     * @param view UI view to display received notifications.
     * @throws SocketException If the UDP socket cannot be opened.
     */
    public NotificationService(int port, ClientView view) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.view = view;
        setDaemon(true);
    }

    /**
     * Returns the actual port being used by the notification service.
     *
     * @return The local UDP port.
     */
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    /**
     * The service's execution loop. Continuously waits for incoming UDP packets
     * and forwards them to the UI view.
     */
    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        while (!isInterrupted() && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                view.showNotification(message);
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    view.showError("UDP Listener error: " + e.getMessage());
                }
                break;
            }
        }
    }

    /**
     * Stops the notification thread and closes the socket.
     */
    @Override
    public void close() {
        interrupt();
        socket.close();
    }
}
