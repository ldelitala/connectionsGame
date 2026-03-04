package com.connections.server;

import com.connections.server.models.User;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.common.dto.*;
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
 * Handles ClientRequests and sends ServerResponses.
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
        ByteBuffer buffer = ByteBuffer.allocate(8192); // Increased buffer size for larger requests
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
            // Peek operation using a simple class
            class OperationPeek { Operation operation; }
            OperationPeek peek = gson.fromJson(json, OperationPeek.class);
            Operation op = (peek != null) ? peek.operation : null;

            if (op == null) {
                sendResponse(clientChannel, ServerResponse.error("Operazione mancante o sconosciuta."));
                return;
            }

            ServerResponse<?> response;
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
                case GET_GAME_INFO:
                case GET_GAME_STATS:
                case GET_PLAYER_STATS:
                case GET_LEADERBOARD:
                    HistoryRequest historyReq = gson.fromJson(json, HistoryRequest.class);
                    response = handleHistoryOperation(clientChannel, op, historyReq);
                    break;
                default:
                    response = ServerResponse.error("Operazione non implementata.");
            }
            sendResponse(clientChannel, response);
        } catch (Exception e) {
            sendResponse(clientChannel, ServerResponse.error("Errore interno del server: " + e.getMessage()));
        }
    }

    private ServerResponse<?> handleUserOperation(SocketChannel channel, Operation op, UserRequest req) {
        switch (op) {
            case REGISTER:
                return userManager.register(req.getUsername(), req.getPassword()) ? 
                    ServerResponse.ok("Registrazione avvenuta con successo.") : 
                    ServerResponse.error("Username già esistente.");
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
                    return ServerResponse.ok("Login effettuato con successo.");
                }
                return ServerResponse.error("Credenziali non valide.");
            case LOGOUT:
                String uname = activeSessions.remove(channel);
                if (uname != null) udpNotifier.unregisterClient(uname);
                return ServerResponse.ok("Logout effettuato.");
            case UPDATE_CREDENTIALS:
                String currentUname = activeSessions.get(channel);
                if (currentUname == null) return ServerResponse.error("Non sei loggato.");
                if (userManager.updateCredentials(currentUname, req.getNewName(), req.getOldPsw(), req.getNewPsw())) {
                    if (req.getNewName() != null) activeSessions.put(channel, req.getNewName());
                    return ServerResponse.ok("Credenziali aggiornate.");
                }
                return ServerResponse.error("Aggiornamento fallito (password errata o nuovo username occupato).");
            default:
                return ServerResponse.error("Operazione utente non supportata.");
        }
    }

    private ServerResponse<?> handleSubmitProposal(SocketChannel channel, GameRequest req) {
        String username = activeSessions.get(channel);
        if (username == null) return ServerResponse.error("Non sei loggato.");
        
        ProposalResultDTO result = sessionManager.submitProposal(username, req.getWords());
        if (result == null) {
            return ServerResponse.error("Proposta non valida (partita finita o gruppo già trovato).");
        }
        return ServerResponse.ok("Proposta elaborata.", result);
    }

    private ServerResponse<?> handleHistoryOperation(SocketChannel channel, Operation op, HistoryRequest req) {
        String username = activeSessions.get(channel);
        if (username == null) return ServerResponse.error("Non sei loggato.");

        switch (op) {
            case GET_GAME_INFO:
                // If gameId is null, use current game
                int gid = (req.getGameId() != null && req.getGameId() != -1) ? 
                          req.getGameId() : sessionManager.getCurrentGame().getGameId();
                GameInfoDTO info = sessionManager.getPersonalGameStatus(username, gid);
                return info != null ? ServerResponse.ok("Info partita " + gid, info) : 
                                     ServerResponse.error("Partita non trovata.");
            case GET_GAME_STATS:
                int sgid = (req.getGameId() != null && req.getGameId() != -1) ? 
                           req.getGameId() : sessionManager.getCurrentGame().getGameId();
                GameStatsDTO stats = sessionManager.getGlobalGameStats(sgid);
                return stats != null ? ServerResponse.ok("Statistiche partita " + sgid, stats) : 
                                      ServerResponse.error("Partita non trovata.");
            case GET_PLAYER_STATS:
                User u = userManager.getUser(username);
                return ServerResponse.ok("Le tue statistiche.", PlayerStatsDTO.create(
                    u.getPuzzlesCompleted(), u.getWinRate(), u.getLossRate(),
                    u.getCurrentStreak(), u.getMaxStreak(), u.getPerfectPuzzles(),
                    u.getMistakeHistogram(), u.getTotalScore()));
            case GET_LEADERBOARD:
                List<User> all = userManager.getAllUsers();
                if (req.getPlayerName() != null) {
                    for (int i = 0; i < all.size(); i++) {
                        User pu = all.get(i);
                        if (pu.getUsername().equals(req.getPlayerName())) 
                            return ServerResponse.ok("Posizione in classifica.", LeaderboardEntryDTO.create(i + 1, pu.getUsername(), pu.getTotalScore()));
                    }
                    return ServerResponse.error("Giocatore non trovato.");
                }
                int k = (req.getTopPlayers() != null) ? req.getTopPlayers() : 10;
                List<LeaderboardEntryDTO> entries = new java.util.ArrayList<>();
                for (int i = 0; i < Math.min(k, all.size()); i++) {
                    User lu = all.get(i);
                    entries.add(LeaderboardEntryDTO.create(i + 1, lu.getUsername(), lu.getTotalScore()));
                }
                return ServerResponse.ok("Classifica globale.", LeaderboardDTO.create(entries));
            default:
                return ServerResponse.error("Operazione di recupero dati non supportata.");
        }
    }

    private void sendResponse(SocketChannel channel, ServerResponse<?> response) {
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
