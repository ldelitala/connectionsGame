package com.connections.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Listens for asynchronous UDP notifications from the server.
 */
public class UdpNotificationListener extends Thread implements AutoCloseable {
    private final int port;
    private final DatagramSocket socket;

    public UdpNotificationListener(int port) throws SocketException {
        this.port = port;
        this.socket = new DatagramSocket(port);
        setDaemon(true);
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public void run() {
        System.out.println("UDP Notification listener started on port " + port);
        byte[] buffer = new byte[1024];
        while (!isInterrupted() && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("\n[NOTIFICATION] " + message);
                System.out.print("> "); // Re-print CLI prompt
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("UDP Listener error: " + e.getMessage());
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
