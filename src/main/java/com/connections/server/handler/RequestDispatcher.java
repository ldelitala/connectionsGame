package com.connections.server.handler;

import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.common.protocol.server_responses.dto.*;
import com.connections.server.network.ClientSession;
import com.connections.server.logic.*;
import com.connections.server.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dispatches incoming JSON requests to the appropriate business logic coordinators.
 * Processing is performed in a worker thread pool to keep the NIO Selector free.
 */
public class RequestDispatcher {
    private final UserCoordinator userCoordinator;
    private final PuzzleCoordinator puzzleCoordinator;
    private final HistoryCoordinator historyCoordinator;
    private final ExecutorService workerPool;
    private final Gson gson = new Gson();

    public RequestDispatcher(UserCoordinator userCoordinator, 
                             PuzzleCoordinator puzzleCoordinator, 
                             HistoryCoordinator historyCoordinator,
                             int poolSize) {
        this.userCoordinator = userCoordinator;
        this.puzzleCoordinator = puzzleCoordinator;
        this.historyCoordinator = historyCoordinator;
        this.workerPool = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Dispatches a raw JSON message for processing.
     */
    public void dispatch(String json, ClientSession session, Runnable onResponseReady) {
        workerPool.execute(() -> {
            ServerResponse<?> response;
            try {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                String opString = obj.get("operation").getAsString();
                Operation op = Operation.valueOf(findOperationConstant(opString));

                response = process(op, json, session);

            } catch (Exception e) {
                response = ServerResponse.error("Internal server error: " + e.getMessage());
            }

            // Serialize and enqueue for NIO write
            String responseJson = gson.toJson(response) + "\n";
            session.enqueueOutput(ByteBuffer.wrap(responseJson.getBytes(StandardCharsets.UTF_8)));
            onResponseReady.run();
        });
    }

    private ServerResponse<?> process(Operation op, String json, ClientSession session) {
        // Shared logic for all operations
        switch (op) {
            case REGISTER:
                UserRequest regReq = gson.fromJson(json, UserRequest.class);
                return userCoordinator.register(regReq.getUsername(), regReq.getPassword()) 
                    ? ServerResponse.ok("Registered successfully.") 
                    : ServerResponse.error("Username already exists.");

            case LOGIN:
                UserRequest loginReq = gson.fromJson(json, UserRequest.class);
                User user = userCoordinator.login(loginReq.getUsername(), loginReq.getPassword(), 
                                                 "localhost", loginReq.getUdpPort());
                if (user != null) {
                    session.setUsername(user.getUsername());
                    return ServerResponse.ok("Login successful.", null);
                }
                return ServerResponse.error("Invalid credentials.");

            case LOGOUT:
                if (session.getUsername() != null) {
                    userCoordinator.logout(session.getUsername());
                    session.setUsername(null);
                    return ServerResponse.ok("Logout successful.");
                }
                return ServerResponse.error("Not logged in.");

            case UPDATE_CREDENTIALS:
                if (session.getUsername() == null) return ServerResponse.forbidden("Login required.");
                UserRequest updReq = gson.fromJson(json, UserRequest.class);
                boolean success = userCoordinator.updateCredentials(updReq.getOldName(), updReq.getOldPsw(), 
                                                                   updReq.getNewName(), updReq.getNewPsw());
                if (success && updReq.getNewName() != null) session.setUsername(updReq.getNewName());
                return success ? ServerResponse.ok("Credentials updated.") : ServerResponse.error("Update failed.");

            case SUBMIT_PROPOSAL:
                if (session.getUsername() == null) return ServerResponse.forbidden("Login required.");
                GameplayRequest propReq = gson.fromJson(json, GameplayRequest.class);
                GuessResultDTO result = puzzleCoordinator.handleProposal(session.getUsername(), propReq.getWords());
                return (result != null) ? ServerResponse.ok("Guess processed.", result) : ServerResponse.error("Invalid or late guess.");

            case GET_PUZZLE_INFO:
                // Works for both GameplayRequest (implicit current) and HistoryRequest (explicit ID)
                Integer infoId = extractPuzzleId(json);
                if (infoId == null || infoId == -1) infoId = puzzleCoordinator.getCurrentPuzzle().getPuzzleId();
                PuzzleStateDTO state = puzzleCoordinator.getPersonalStatus(session.getUsername(), infoId);
                return (state != null) ? ServerResponse.ok("Puzzle info retrieved.", state) : ServerResponse.error("Puzzle not found or no participation recorded.");

            case GET_PUZZLE_STATS:
                Integer statsId = extractPuzzleId(json);
                if (statsId == null || statsId == -1) statsId = puzzleCoordinator.getCurrentPuzzle().getPuzzleId();
                PuzzleStatsDTO stats = puzzleCoordinator.getGlobalStats(statsId);
                return (stats != null) ? ServerResponse.ok("Puzzle stats retrieved.", stats) : ServerResponse.error("Stats not available.");

            case GET_PLAYER_STATS:
                if (session.getUsername() == null) return ServerResponse.forbidden("Login required.");
                User u = userCoordinator.getUser(session.getUsername());
                return ServerResponse.ok("Player stats retrieved.", PlayerStatsDTO.create(
                        u.getPuzzlesCompleted(),
                        u.getWinRate(),
                        u.getLossRate(),
                        u.getCurrentStreak(),
                        u.getMaxStreak(),
                        u.getPerfectPuzzles(),
                        u.getMistakeHistogram(),
                        u.getTotalScore()
                ));

            case GET_LEADERBOARD:
                HistoryRequest leadReq = gson.fromJson(json, HistoryRequest.class);
                if (leadReq.getPlayerName() != null) {
                    LeaderboardEntryDTO rank = historyCoordinator.getPlayerRank(leadReq.getPlayerName());
                    return (rank != null) ? ServerResponse.ok("Rank retrieved.", rank) : ServerResponse.error("Player not found.");
                }
                return ServerResponse.ok("Leaderboard retrieved.", historyCoordinator.getGlobalLeaderboard(leadReq.getTopPlayers()));

            default:
                return ServerResponse.error("Unsupported operation.");
        }
    }

    private Integer extractPuzzleId(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (obj.has("gameId") && !obj.get("gameId").isJsonNull()) {
            return obj.get("gameId").getAsInt();
        }
        return null;
    }

    private String findOperationConstant(String serializedName) {
        for (Operation op : Operation.values()) {
            try {
                java.lang.reflect.Field field = Operation.class.getField(op.name());
                com.google.gson.annotations.SerializedName sn = field.getAnnotation(com.google.gson.annotations.SerializedName.class);
                if (sn != null && sn.value().equals(serializedName)) return op.name();
            } catch (Exception ignored) {}
        }
        return serializedName.toUpperCase(); // Fallback
    }

    public void handleDisconnect(String username) {
        userCoordinator.logout(username);
    }
}
