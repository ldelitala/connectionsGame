package com.connections.server;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles asynchronous UDP notifications to clients.
 */
public class UdpNotifier {
    private final DatagramSocket socket;
    
    // Map of username -> ClientEndpoint
    private final Map<String, ClientEndpoint> clients = new ConcurrentHashMap<>();

    public UdpNotifier() throws SocketException {
        this.socket = new DatagramSocket();
    }

    public void registerClient(String username, String host, int port) {
        try {
            clients.put(username, new ClientEndpoint(InetAddress.getByName(host), port));
        } catch (UnknownHostException e) {
            System.err.println("Error registering UDP client " + username + ": " + e.getMessage());
        }
    }

    public void removeClient(String username) {
        clients.remove(username);
    }

    public void notifyAll(String message) {
        byte[] buffer = message.getBytes();
        for (ClientEndpoint endpoint : clients.values()) {
            send(buffer, endpoint);
        }
    }

    public void notifyUser(String username, String message) {
        ClientEndpoint endpoint = clients.get(username);
        if (endpoint != null) {
            send(message.getBytes(), endpoint);
        }
    }

    private void send(byte[] buffer, ClientEndpoint endpoint) {
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, endpoint.address, endpoint.port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending UDP packet: " + e.getMessage());
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
