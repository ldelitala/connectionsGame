package com.connections.server.handler;

import com.connections.common.protocol.Operation;
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
 * Uses a triple-dispatcher pattern to route requests and returns specialized response types.
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

    public void dispatch(String json, ClientSession session, Runnable onResponseReady) {
        workerPool.execute(() -> {
            ServerResponse<?> response;
            Operation op = null;
            try {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                String opString = obj.get("operation").getAsString();
                op = Operation.valueOf(findOperationConstant(opString));

                Class<? extends ClientRequest> reqClass = op.getRequestClass();
                if (reqClass == null) {
                    reqClass = obj.has("gameId") ? HistoryRequest.class : GameplayRequest.class;
                }
                
                ClientRequest request = gson.fromJson(json, reqClass);
                response = route(request, op, session);

            } catch (Exception e) {
                // If operation was identified, return a specialized error if possible
                response = (op != null) 
                    ? UserResponse.error(op, "Internal server error: " + e.getMessage())
                    : UserResponse.error(Operation.REGISTER, "Critical protocol error: " + e.getMessage());
            }

            String responseJson = gson.toJson(response) + "\n";
            session.enqueueOutput(ByteBuffer.wrap(responseJson.getBytes(StandardCharsets.UTF_8)));
            onResponseReady.run();
        });
    }

    private ServerResponse<?> route(ClientRequest request, Operation op, ClientSession session) {
        if (request instanceof UserRequest) return handleUser((UserRequest) request, op, session);
        if (request instanceof GameplayRequest) return handleGameplay((GameplayRequest) request, op, session);
        if (request instanceof HistoryRequest) return handleHistory((HistoryRequest) request, op, session);
        return UserResponse.error(op, "Unsupported request type.");
    }

    private ServerResponse<?> handleUser(UserRequest req, Operation op, ClientSession session) {
        switch (op) {
            case REGISTER:
                return userCoordinator.register(req.getUsername(), req.getPassword()) 
                    ? UserResponse.ok(op, "Registered successfully.") 
                    : UserResponse.error(op, "Username already exists.");

            case LOGIN:
                User user = userCoordinator.login(req.getUsername(), req.getPassword(), 
                                                 "localhost", req.getUdpPort());
                if (user != null) {
                    session.setUsername(user.getUsername());
                    return UserResponse.ok(op, "Login successful.");
                }
                return UserResponse.error(op, "Invalid credentials.");

            case LOGOUT:
                if (session.getUsername() != null) {
                    userCoordinator.logout(session.getUsername());
                    session.setUsername(null);
                    return UserResponse.ok(op, "Logout successful.");
                }
                return UserResponse.error(op, "Not logged in.");

            case UPDATE_CREDENTIALS:
                if (session.getUsername() == null) return UserResponse.forbidden(op, "Login required.");
                boolean success = userCoordinator.updateCredentials(req.getOldName(), req.getOldPsw(), 
                                                                   req.getNewName(), req.getNewPsw());
                if (success && req.getNewName() != null) session.setUsername(req.getNewName());
                return success ? UserResponse.ok(op, "Credentials updated.") : UserResponse.error(op, "Update failed.");

            default: return UserResponse.error(op, "Invalid user operation.");
        }
    }

    private ServerResponse<?> handleGameplay(GameplayRequest req, Operation op, ClientSession session) {
        if (session.getUsername() == null) return GameplayResponse.error(op, "Login required.");
        
        switch (op) {
            case SUBMIT_PROPOSAL:
                GuessResultDTO result = puzzleCoordinator.handleProposal(session.getUsername(), req.getWords());
                return (result != null) ? GameplayResponse.ok(op, "Guess processed.", result) : GameplayResponse.error(op, "Invalid or late guess.");

            case GET_PUZZLE_INFO:
                PuzzleStateDTO state = puzzleCoordinator.getPersonalStatus(session.getUsername(), 
                        puzzleCoordinator.getCurrentPuzzle().getPuzzleId());
                return (state != null) ? GameplayResponse.ok(op, state) : GameplayResponse.error(op, "Puzzle not found.");

            case GET_PUZZLE_STATS:
                PuzzleStatsDTO stats = puzzleCoordinator.getGlobalStats(puzzleCoordinator.getCurrentPuzzle().getPuzzleId());
                return (stats != null) ? GameplayResponse.ok(op, stats) : GameplayResponse.error(op, "Stats not available.");

            default: return GameplayResponse.error(op, "Invalid gameplay operation.");
        }
    }

    private ServerResponse<?> handleHistory(HistoryRequest req, Operation op, ClientSession session) {
        switch (op) {
            case GET_PUZZLE_INFO:
                PuzzleStateDTO state = puzzleCoordinator.getPersonalStatus(session.getUsername(), req.getPuzzleId());
                return (state != null) ? HistoryResponse.ok(op, state) : HistoryResponse.error(op, "History record not found.");

            case GET_PUZZLE_STATS:
                PuzzleStatsDTO stats = puzzleCoordinator.getGlobalStats(req.getPuzzleId());
                return (stats != null) ? HistoryResponse.ok(op, stats) : HistoryResponse.error(op, "Stats not available.");

            case GET_PLAYER_STATS:
                if (session.getUsername() == null) return HistoryResponse.error(op, "Login required.");
                User u = userCoordinator.getUser(session.getUsername());
                return HistoryResponse.ok(op, PlayerStatsDTO.create(
                        u.getPuzzlesCompleted(), u.getWinRate(), u.getLossRate(), u.getCurrentStreak(),
                        u.getMaxStreak(), u.getPerfectPuzzles(), u.getMistakeHistogram(), u.getTotalScore()));

            case GET_LEADERBOARD:
                if (req.getPlayerName() != null) {
                    LeaderboardEntryDTO rank = historyCoordinator.getPlayerRank(req.getPlayerName());
                    return (rank != null) ? HistoryResponse.ok(op, rank) : HistoryResponse.error(op, "Player not found.");
                }
                return HistoryResponse.ok(op, historyCoordinator.getGlobalLeaderboard(req.getTopPlayers()));

            default: return HistoryResponse.error(op, "Invalid history operation.");
        }
    }

    private String findOperationConstant(String serializedName) {
        for (Operation op : Operation.values()) {
            try {
                java.lang.reflect.Field field = Operation.class.getField(op.name());
                com.google.gson.annotations.SerializedName sn = field.getAnnotation(com.google.gson.annotations.SerializedName.class);
                if (sn != null && sn.value().equals(serializedName)) return op.name();
            } catch (Exception ignored) {}
        }
        return serializedName.toUpperCase();
    }

    public void handleDisconnect(String username) {
        if (username != null) userCoordinator.logout(username);
    }
}
