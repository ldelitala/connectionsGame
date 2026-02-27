package com.connections.server;

import com.connections.common.Request;
import com.connections.common.Response;
import com.connections.common.User;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NIO-based TCP server for Connections Game.
 */
public class ConnectionsServer {
    private final ServerConfig config;
    private final UserManager userManager;
    private final GameSessionManager sessionManager;
    private final UdpNotifier udpNotifier;
    private final ExecutorService threadPool;
    private final Gson gson = new Gson();
    
    // Tracks active connections and their logged-in usernames
    private final Map<SocketChannel, String> activeSessions = new ConcurrentHashMap<>();

    public ConnectionsServer(ServerConfig config, UserManager userManager, GameSessionManager sessionManager, UdpNotifier udpNotifier) {
        this.config = config;
        this.userManager = userManager;
        this.sessionManager = sessionManager;
        this.udpNotifier = udpNotifier;
        this.threadPool = Executors.newFixedThreadPool(config.getThreadPoolSize());
    }

    public void start() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(config.getTcpPort()));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("TCP Server started on port " + config.getTcpPort());

        while (!Thread.currentThread().isInterrupted()) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();

                if (key.isAcceptable()) {
                    handleAccept(serverChannel, selector);
                } else if (key.isReadable()) {
                    handleRead(key);
                }
                iter.remove();
            }
        }
    }

    private void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted connection from " + clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        
        try {
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                closeConnection(clientChannel);
                return;
            }

            String message = new String(buffer.array(), 0, bytesRead).trim();
            if (message.isEmpty()) return;

            // Offload processing to thread pool
            threadPool.submit(() -> processRequest(clientChannel, message));

        } catch (IOException e) {
            closeConnection(clientChannel);
        }
    }

    private void processRequest(SocketChannel clientChannel, String json) {
        try {
            Request request = gson.fromJson(json, Request.class);
            Response response;

            switch (request.getOperation()) {
                case "register":
                    response = handleRegister(request);
                    break;
                case "login":
                    response = handleLogin(clientChannel, request);
                    break;
                case "logout":
                    response = handleLogout(clientChannel);
                    break;
                case "submitProposal":
                    response = handleSubmitProposal(clientChannel, request);
                    break;
                case "requestGameInfo":
                    response = handleRequestGameInfo(clientChannel, request);
                    break;
                case "requestGameStats":
                    response = handleRequestGameStats(clientChannel, request);
                    break;
                case "requestPlayerStats":
                    response = handleRequestPlayerStats(clientChannel);
                    break;
                case "requestLeaderboard":
                    response = handleRequestLeaderboard(clientChannel, request);
                    break;
                case "updateCredentials":
                    response = handleUpdateCredentials(clientChannel, request);
                    break;
                default:
                    response = Response.error("Unknown operation: " + request.getOperation());
            }

            sendResponse(clientChannel, response);

        } catch (Exception e) {
            sendResponse(clientChannel, Response.error("Internal server error: " + e.getMessage()));
        }
    }

    private Response handleUpdateCredentials(SocketChannel channel, Request req) {
        String username = activeSessions.get(channel);
        if (username == null) return Response.error("Not logged in.");
        
        if (userManager.updateCredentials(username, req.getNewUsername(), req.getOldPassword(), req.getNewPassword())) {
            if (req.getNewUsername() != null && !req.getNewUsername().equals(username)) {
                activeSessions.put(channel, req.getNewUsername());
            }
            return Response.ok("Credentials updated successfully.");
        } else {
            return Response.error("Update failed. Check your current password or if username is taken.");
        }
    }

    private Response handleRegister(Request req) {
        if (userManager.register(req.getUsername(), req.getPassword())) {
            return Response.ok("Registration successful.");
        } else {
            return Response.error("Username already exists.");
        }
    }

    private Response handleLogin(SocketChannel channel, Request req) {
        User user = userManager.authenticate(req.getUsername(), req.getPassword());
        if (user != null) {
            String username = user.getUsername();
            activeSessions.put(channel, username);
            
            // Register UDP endpoint if provided
            if (req.getUdpPort() != null) {
                try {
                    InetAddress address = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
                    udpNotifier.registerClient(username, address, req.getUdpPort());
                } catch (IOException e) {
                    System.err.println("Failed to get remote address for UDP registration: " + e.getMessage());
                }
            }
            
            return Response.ok("Login successful.");
        } else {
            return Response.error("Invalid credentials.");
        }
    }

    private Response handleLogout(SocketChannel channel) {
        String username = activeSessions.remove(channel);
        if (username != null) {
            udpNotifier.unregisterClient(username);
        }
        return Response.ok("Logged out.");
    }

    private Response handleSubmitProposal(SocketChannel channel, Request req) {
        String username = activeSessions.get(channel);
        if (username == null) return Response.error("Not logged in.");
        
        Map<String, Object> result = sessionManager.submitProposal(username, req.getWords());
        return Response.ok("Proposal processed.", result);
    }

    private Response handleRequestGameInfo(SocketChannel channel, Request req) {
        String username = activeSessions.get(channel);
        if (username == null) return Response.error("Not logged in.");
        
        int gameId = (req.getGameId() != null) ? req.getGameId() : sessionManager.getCurrentGame().getGameId();
        Map<String, Object> status = sessionManager.getPersonalGameStatus(username, gameId);
        
        if (status != null) {
            return Response.ok("Game status for ID " + gameId, status);
        } else {
            return Response.error("Game info not found for ID " + gameId);
        }
    }

    private Response handleRequestGameStats(SocketChannel channel, Request req) {
        if (!activeSessions.containsKey(channel)) return Response.error("Not logged in.");
        
        int gameId = (req.getGameId() != null) ? req.getGameId() : sessionManager.getCurrentGame().getGameId();
        Map<String, Object> stats = sessionManager.getGlobalGameStats(gameId);
        
        if (stats != null) {
            return Response.ok("Global stats for ID " + gameId, stats);
        } else {
            return Response.error("Game statistics not found for ID " + gameId);
        }
    }

    private Response handleRequestPlayerStats(SocketChannel channel) {
        String username = activeSessions.get(channel);
        if (username == null) return Response.error("Not logged in.");
        User user = userManager.getUser(username);
        Map<String, Object> stats = new HashMap<>();
        stats.put("puzzlesCompleted", user.getPuzzlesCompleted());
        stats.put("winRate", user.getWinRate());
        stats.put("lossRate", user.getLossRate());
        stats.put("currentStreak", user.getCurrentStreak());
        stats.put("maxStreak", user.getMaxStreak());
        stats.put("perfectPuzzles", user.getPerfectPuzzles());
        stats.put("mistakeHistogram", user.getMistakeHistogram());
        stats.put("totalScore", user.getTotalScore());
        return Response.ok("Player stats.", stats);
    }

    private Response handleRequestLeaderboard(SocketChannel channel, Request req) {
        if (!activeSessions.containsKey(channel)) return Response.error("Not logged in.");
        
        if (req.getPlayerName() != null) {
            // Position of a specific player
            List<User> all = userManager.getAllUsers();
            int rank = -1;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getUsername().equals(req.getPlayerName())) {
                    rank = i + 1;
                    break;
                }
            }
            if (rank != -1) {
                return Response.ok("Player rank.", rank);
            } else {
                return Response.error("Player not found.");
            }
        } else {
            int k = (req.getTopPlayers() != null) ? req.getTopPlayers() : 10;
            return Response.ok("Leaderboard.", userManager.getTopK(k));
        }
    }

    private void sendResponse(SocketChannel channel, Response response) {
        try {
            String json = gson.toJson(response);
            channel.write(ByteBuffer.wrap((json + "\n").getBytes()));
        } catch (IOException e) {
            closeConnection(channel);
        }
    }

    private void closeConnection(SocketChannel channel) {
        try {
            String username = activeSessions.remove(channel);
            if (username != null) {
                udpNotifier.unregisterClient(username);
            }
            channel.close();
            System.out.println("Closed connection.");
        } catch (IOException ignored) {}
    }
}
