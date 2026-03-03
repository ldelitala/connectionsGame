package com.connections.server;

import com.connections.common.models.WordGroup;
import com.connections.server.models.User;
import com.connections.server.models.Game;
import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import com.connections.common.protocol.responses.dto.*;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
            threadPool.submit(() -> processRequest(clientChannel, message));
        } catch (IOException e) {
            closeConnection(clientChannel);
        }
    }

    private void processRequest(SocketChannel clientChannel, String json) {
        try {
            class OperationPeek { Operation operation; }
            OperationPeek peek = gson.fromJson(json, OperationPeek.class);
            Operation op = (peek != null) ? peek.operation : null;

            if (op == null) {
                sendResponse(clientChannel, Response.error("Missing or unknown operation."));
                return;
            }

            Response<?> response;
            switch (op) {
                case REGISTER:
                case LOGIN:
                case LOGOUT:
                case UPDATE_CREDENTIALS:
                    UserRequest userReq = gson.fromJson(json, UserRequest.class);
                    response = handleUserOperation(clientChannel, op, userReq);
                    break;
                case SUBMIT_PROPOSAL:
                    GameRequest gameReq = gson.fromJson(json, GameRequest.class);
                    response = handleSubmitProposal(clientChannel, gameReq);
                    break;
                case REQUEST_GAME_INFO:
                case REQUEST_GAME_STATS:
                case REQUEST_PLAYER_STATS:
                case REQUEST_LEADERBOARD:
                    InfoRequest infoReq = gson.fromJson(json, InfoRequest.class);
                    response = handleInfoOperation(clientChannel, op, infoReq);
                    break;
                default:
                    response = Response.error("Operation not implemented.");
            }
            sendResponse(clientChannel, response);
        } catch (Exception e) {
            sendResponse(clientChannel, Response.error("Internal server error: " + e.getMessage()));
        }
    }

    private Response<?> handleUserOperation(SocketChannel channel, Operation op, UserRequest req) {
        switch (op) {
            case REGISTER:
                return userManager.register(req.getUsername(), req.getPassword()) ? 
                    Response.ok("Registration successful.") : Response.error("Username exists.");
            case LOGIN:
                User user = userManager.authenticate(req.getUsername(), req.getPassword());
                if (user != null) {
                    activeSessions.put(channel, user.getUsername());
                    if (req.getUdpPort() != null) {
                        try {
                            InetAddress addr = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
                            udpNotifier.registerClient(user.getUsername(), addr, req.getUdpPort());
                        } catch (IOException ignored) {}
                    }
                    return Response.ok("Login successful.");
                }
                return Response.error("Invalid credentials.");
            case LOGOUT:
                String uname = activeSessions.remove(channel);
                if (uname != null) udpNotifier.unregisterClient(uname);
                return Response.ok("Logged out.");
            case UPDATE_CREDENTIALS:
                String currentUname = activeSessions.get(channel);
                if (currentUname == null) return Response.error("Not logged in.");
                if (userManager.updateCredentials(currentUname, req.getNewName(), req.getOldPsw(), req.getNewPsw())) {
                    if (req.getNewName() != null) activeSessions.put(channel, req.getNewName());
                    return Response.ok("Updated.");
                }
                return Response.error("Update failed.");
            default:
                return Response.error("Unsupported user operation.");
        }
    }

    private Response<?> handleSubmitProposal(SocketChannel channel, GameRequest req) {
        String username = activeSessions.get(channel);
        if (username == null) return Response.error("Not logged in.");
        
        ProposalResultDTO result = sessionManager.submitProposal(username, req.getWords());
        if (result == null) {
            return Response.error("Invalid proposal request (game finished, time up, or group already found).");
        }
        return Response.ok("Processed.", result);
    }

    private Response<?> handleInfoOperation(SocketChannel channel, Operation op, InfoRequest req) {
        String username = activeSessions.get(channel);
        if (username == null) return Response.error("Not logged in.");

        switch (op) {
            case REQUEST_GAME_INFO:
                int gid = (req.getGameId() != null) ? req.getGameId() : sessionManager.getCurrentGame().getGameId();
                GameInfoDTO info = sessionManager.getPersonalGameStatus(username, gid);
                return info != null ? Response.ok("Info for " + gid, info) : Response.error("Not found.");
            case REQUEST_GAME_STATS:
                int sgid = (req.getGameId() != null) ? req.getGameId() : sessionManager.getCurrentGame().getGameId();
                GameStatsDTO stats = sessionManager.getGlobalGameStats(sgid);
                return stats != null ? Response.ok("Stats for " + sgid, stats) : Response.error("Not found.");
            case REQUEST_PLAYER_STATS:
                User u = userManager.getUser(username);
                return Response.ok("Player stats.", PlayerStatsDTO.create(
                    u.getPuzzlesCompleted(), u.getWinRate(), u.getLossRate(),
                    u.getCurrentStreak(), u.getMaxStreak(), u.getPerfectPuzzles(),
                    u.getMistakeHistogram(), u.getTotalScore()));
            case REQUEST_LEADERBOARD:
                List<User> all = userManager.getAllUsers();
                if (req.getPlayerName() != null) {
                    for (int i = 0; i < all.size(); i++) {
                        User pu = all.get(i);
                        if (pu.getUsername().equals(req.getPlayerName())) 
                            return Response.ok("Rank.", LeaderboardEntryDTO.create(i + 1, pu.getUsername(), pu.getTotalScore()));
                    }
                    return Response.error("Not found.");
                }
                int k = (req.getTopPlayers() != null) ? req.getTopPlayers() : all.size();
                List<LeaderboardEntryDTO> entries = new java.util.ArrayList<>();
                for (int i = 0; i < Math.min(k, all.size()); i++) {
                    User lu = all.get(i);
                    entries.add(LeaderboardEntryDTO.create(i + 1, lu.getUsername(), lu.getTotalScore()));
                }
                return Response.ok("Leaderboard.", LeaderboardDTO.create(entries));
            default:
                return Response.error("Unsupported info operation.");
        }
    }

    private void sendResponse(SocketChannel channel, Response<?> response) {
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
            if (username != null) udpNotifier.unregisterClient(username);
            channel.close();
        } catch (IOException ignored) {}
    }
}
