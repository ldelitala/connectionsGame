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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance Request Dispatcher.
 * Routes requests to specialized coordinators and handles session state.
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
     * Executes the request lifecycle in a worker thread.
     */
    public void execute(String json, ClientSession session, Runnable onResponseReady) {
        workerPool.execute(() -> {
            ServerResponse<?> response;
            Operation op = null;
            try {
                // 1. Identify true operation (resolving ambiguity)
                op = resolveOperation(json);
                
                // 2. Deserialize to specialized class
                ClientRequest request = gson.fromJson(json, op.getRequestClass());
                
                // 3. Route using the resolved operation
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
        if (req instanceof UserRequest) {
            return userCoordinator.handle((UserRequest) req, op, username);
        } else if (req instanceof GameplayRequest) {
            return puzzleCoordinator.handle((GameplayRequest) req, op, username);
        } else if (req instanceof HistoryRequest) {
            return historyCoordinator.handle((HistoryRequest) req, op, username, puzzleCoordinator);
        }
        return UserResponse.error(op, "Unsupported request type.");
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

    private static class RequestHeader {
        Operation operation;
        @SerializedName("gameId") Integer gameId;
    }

    public void handleDisconnect(String username) {
        if (username != null) userCoordinator.logout(username);
    }
}
