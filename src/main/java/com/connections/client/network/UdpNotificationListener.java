package com.connections.client.network;

import com.connections.client.ui.ClientView;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Listens for asynchronous UDP notifications from the server.
 */
public class UdpNotificationListener extends Thread implements AutoCloseable {
    private final DatagramSocket socket;
    private final ClientView view;

    public UdpNotificationListener(int port, ClientView view) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.view = view;
        setDaemon(true);
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

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

    @Override
    public void close() {
        interrupt();
        socket.close();
    }
}
