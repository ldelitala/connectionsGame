package com.connections.server.network;

import com.connections.common.notifications.*;
import com.connections.common.protocol.server_responses.dto.LeaderboardDTO;
import com.connections.common.protocol.server_responses.dto.PuzzleStateDTO;
import com.connections.common.protocol.server_responses.dto.PuzzleStatsDTO;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles asynchronous UDP notifications to clients using JSON format.
 * Now correctly uses the polymorphic Notification DTOs from the common package.
 */
public class UdpNotifier {
    private final DatagramSocket socket;
    private final Gson gson = new Gson();
    
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

    public void unregisterClient(String username) {
        clients.remove(username);
    }

    public void updateClientKey(String oldName, String newName) {
        ClientEndpoint endpoint = clients.remove(oldName);
        if (endpoint != null) {
            clients.put(newName, endpoint);
        }
    }

    // --- NOTIFICATION METHODS ---

    /**
     * Broadcasts a new puzzle notification to all registered clients.
     * Uses NewPuzzleNotification from common package.
     */
    public void broadcastNewPuzzle(int puzzleId, List<String> words) {
        NewPuzzleNotification n = NewPuzzleNotification.create(puzzleId, words);
        broadcast(gson.toJson(n));
    }

    /**
     * Broadcasts a puzzle end notification to all registered clients.
     * Uses PuzzleEndNotification from common package.
     */
    public void broadcastPuzzleEnd(int puzzleId, LeaderboardDTO leaderboard, PuzzleStatsDTO stats) {
        PuzzleEndNotification n = PuzzleEndNotification.create(leaderboard, stats);
        broadcast(gson.toJson(n));
    }

    /**
     * Sends a private personal summary notification to a specific player.
     * Uses PlayerPuzzleSummaryNotification from common package.
     */
    public void sendPlayerSummary(String username, PuzzleStateDTO summary) {
        PlayerPuzzleSummaryNotification n = PlayerPuzzleSummaryNotification.create(summary);
        notifyUser(username, gson.toJson(n));
    }

    // --- PRIVATE HELPERS ---

    private void broadcast(String message) {
        byte[] buffer = message.getBytes();
        for (ClientEndpoint endpoint : clients.values()) {
            send(buffer, endpoint);
        }
    }

    private void notifyUser(String username, String message) {
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
