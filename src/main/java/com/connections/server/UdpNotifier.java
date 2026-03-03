package com.connections.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles asynchronous UDP notifications to clients.
 */
public class UdpNotifier {
    private DatagramSocket socket;
    // Map: username -> client UDP address/port
    private final Map<String, ClientEndpoint> clientEndpoints = new ConcurrentHashMap<>();

    public UdpNotifier() {
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Could not start UDP notifier: " + e.getMessage());
        }
    }

    public void registerClient(String username, InetAddress address, int port) {
        clientEndpoints.put(username, new ClientEndpoint(address, port));
        System.out.println("Registered UDP endpoint for " + username + " at " + address + ":" + port);
    }

    public void unregisterClient(String username) {
        clientEndpoints.remove(username);
    }

    /**
     * Sends a UDP notification to all registered clients.
     * 
     * @param message the JSON message to send
     */
    public void notifyAll(String message) {
        byte[] buffer = message.getBytes();
        for (ClientEndpoint endpoint : clientEndpoints.values()) {
            sendToEndpoint(buffer, endpoint);
        }
    }

    /**
     * Sends a UDP notification to a specific user.
     * 
     * @param username the target user
     * @param message the JSON message to send
     */
    public void notifyUser(String username, String message) {
        ClientEndpoint endpoint = clientEndpoints.get(username);
        if (endpoint != null) {
            sendToEndpoint(message.getBytes(), endpoint);
        }
    }

    private void sendToEndpoint(byte[] buffer, ClientEndpoint endpoint) {
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, endpoint.address, endpoint.port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Failed to send UDP notification to " + endpoint.address + ": " + e.getMessage());
        }
    }

    private static class ClientEndpoint {
        final InetAddress address;
        final int port;

        ClientEndpoint(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
    }
}
