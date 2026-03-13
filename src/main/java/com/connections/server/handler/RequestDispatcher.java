package com.connections.server.handler;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.server.network.ClientSession;
import com.connections.server.logic.*;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance Request Dispatcher.
 * Routes requests using a functional routing table to avoid instanceof checks.
 */
public class RequestDispatcher {
    private final UserCoordinator userCoordinator;
    private final PuzzleCoordinator puzzleCoordinator;
    private final HistoryCoordinator historyCoordinator;
    private final ExecutorService workerPool;
    private final Gson gson = new Gson();

    /** Table mapping request classes to their respective handler logic. */
    private final Map<Class<? extends ClientRequest>, RequestHandler> routingTable = new HashMap<>();

    public RequestDispatcher(UserCoordinator userCoordinator, 
                             PuzzleCoordinator puzzleCoordinator, 
                             HistoryCoordinator historyCoordinator,
                             int poolSize) {
        this.userCoordinator = userCoordinator;
        this.puzzleCoordinator = puzzleCoordinator;
        this.historyCoordinator = historyCoordinator;
        this.workerPool = Executors.newFixedThreadPool(poolSize);
        initializeRoutingTable();
    }

    private void initializeRoutingTable() {
        routingTable.put(UserRequest.class, (req, op, user) -> 
            userCoordinator.handle((UserRequest) req, op, user));
            
        routingTable.put(GameplayRequest.class, (req, op, user) -> 
            puzzleCoordinator.handle((GameplayRequest) req, op, user));
            
        routingTable.put(HistoryRequest.class, (req, op, user) -> 
            historyCoordinator.handle((HistoryRequest) req, op, user, puzzleCoordinator));
    }

    /**
     * Executes the request lifecycle in a worker thread.
     */
    public void execute(String json, ClientSession session, Runnable onResponseReady) {
        workerPool.execute(() -> {
            ServerResponse<?> response;
            Operation op = null;
            try {
                op = resolveOperation(json);
                ClientRequest request = gson.fromJson(json, op.getRequestClass());
                
                response = routeToCoordinator(request, op, session.getUsername());
                updateSessionState(session, request, op, response);

            } catch (Exception e) {
                response = createErrorResponse(op, "Internal error: " + e.getMessage());
            }

            session.enqueueOutput(serializeResponse(response));
            onResponseReady.run();
        });
    }

    private Operation resolveOperation(String json) {
        RequestHeader header = gson.fromJson(json, RequestHeader.class);
        Operation op = header.operation;
        if (op == null) throw new IllegalArgumentException("Unknown operation");

        if (header.gameId != null) {
            if (op == Operation.GET_PUZZLE_INFO) return Operation.GET_PAST_PUZZLE_INFO;
            if (op == Operation.GET_PUZZLE_STATS) return Operation.GET_PAST_PUZZLE_STATS;
        }
        return op;
    }

    private ServerResponse<?> routeToCoordinator(ClientRequest req, Operation op, String username) {
        RequestHandler handler = routingTable.get(req.getClass());
        if (handler == null) {
            return UserResponse.error(op, "No coordinator registered for request type: " + req.getClass().getSimpleName());
        }
        return handler.handle(req, op, username);
    }

    private ByteBuffer serializeResponse(ServerResponse<?> response) {
        String json = gson.toJson(response) + "\n";
        return ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
    }

    private void updateSessionState(ClientSession session, ClientRequest request, Operation op, ServerResponse<?> response) {
        if (response.getStatus() == Status.OK) {
            if (op == Operation.LOGIN) {
                session.setUsername(((UserRequest)request).getUsername());
            } else if (op == Operation.LOGOUT) {
                session.setUsername(null);
            } else if (op == Operation.UPDATE_CREDENTIALS) {
                String newName = ((UserRequest)request).getNewName();
                if (newName != null && !newName.equals("-")) session.setUsername(newName);
            }
        }
    }

    private ServerResponse<?> createErrorResponse(Operation op, String msg) {
        if (op == null) return UserResponse.error(Operation.REGISTER, msg);
        Class<? extends ServerResponse> resClass = op.getResponseClass();
        if (resClass == UserResponse.class) return UserResponse.error(op, msg);
        if (resClass == GameplayResponse.class) return GameplayResponse.error(op, msg);
        if (resClass == HistoryResponse.class) return HistoryResponse.error(op, msg);
        return UserResponse.error(op, msg);
    }

    /**
     * Functional interface for dynamic request routing.
     */
    @FunctionalInterface
    private interface RequestHandler {
        ServerResponse<?> handle(ClientRequest request, Operation op, String username);
    }

    private static class RequestHeader {
        Operation operation;
        @SerializedName("gameId") Integer gameId;
    }

    public void handleDisconnect(String username) {
        if (username != null) userCoordinator.logout(username);
    }
}
