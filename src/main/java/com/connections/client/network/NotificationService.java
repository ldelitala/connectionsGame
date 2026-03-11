package com.connections.client.network;

import com.connections.client.ui.ClientView;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Listens for asynchronous UDP notifications from the server.
 * Operates in a background daemon thread.
 */
public class NotificationService extends Thread implements AutoCloseable {

    /** Buffer size for incoming UDP packets (4KB to handle large JSON notifications). */
    private static final int BUFFER_SIZE = 4096;

    private final DatagramSocket socket;
    private final ClientView view;

    /**
     * Constructs a new NotificationService on the specified port.
     * 
     * @param port the local UDP port to bind to.
     * @param view the UI view to display notifications.
     * @throws SocketException if the socket could not be opened or bound.
     */
    public NotificationService(int port, ClientView view) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.view = view;
        setDaemon(true);
        setName("NotificationService-Thread");
    }

    /**
     * Returns the actual local port this service is bound to.
     * 
     * @return the local UDP port.
     */
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    /**
     * Continuously listens for UDP packets until interrupted or the socket is closed.
     */
    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (!isInterrupted() && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // Convert packet data to string, ensuring we only read the actual bytes received
                String message = new String(packet.getData(), 0, packet.getLength());
                
                // Dispatch notification to the UI view
                view.showNotification(message);
                
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    view.showError("Notification service error: " + e.getMessage());
                }
                break;
            }
        }
    }

    /**
     * Gracefully stops the service and closes the UDP socket.
     */
    @Override
    public void close() {
        interrupt();
        socket.close();
    }
}
