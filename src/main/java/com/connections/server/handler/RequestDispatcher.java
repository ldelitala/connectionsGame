package com.connections.server.handler;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.server.network.ClientSession;
import com.connections.server.logic.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
                // 1. Pipeline: Deserialize -> Route -> Session Management
                ClientRequest request = deserializeRequest(json);
                op = request.getOperation();
                
                response = routeToCoordinator(request, session.getUsername());
                
                updateSessionState(session, request, response);

            } catch (Exception e) {
                response = createErrorResponse(op, "Internal error: " + e.getMessage());
            }

            // 2. Pipeline: Serialize -> Enqueue -> Callback
            session.enqueueOutput(serializeResponse(response));
            onResponseReady.run();
        });
    }

    private ClientRequest deserializeRequest(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String opString = obj.get("operation").getAsString();
        Operation op = Operation.valueOf(findOperationConstant(opString));

        Class<? extends ClientRequest> reqClass = op.getRequestClass();
        if (reqClass == null) {
            reqClass = obj.has("gameId") ? HistoryRequest.class : GameplayRequest.class;
        }
        return gson.fromJson(json, reqClass);
    }

    private ServerResponse<?> routeToCoordinator(ClientRequest req, String username) {
        if (req instanceof UserRequest) {
            return userCoordinator.handle((UserRequest) req, username);
        } else if (req instanceof GameplayRequest) {
            return puzzleCoordinator.handle((GameplayRequest) req, username);
        } else if (req instanceof HistoryRequest) {
            return historyCoordinator.handle((HistoryRequest) req, username, puzzleCoordinator);
        }
        return UserResponse.error(req.getOperation(), "Unsupported request type.");
    }

    private ByteBuffer serializeResponse(ServerResponse<?> response) {
        String json = gson.toJson(response) + "\n";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.wrap(bytes);
    }

    private void updateSessionState(ClientSession session, ClientRequest request, ServerResponse<?> response) {
        if (response.getStatus() == Status.OK) {
            Operation op = request.getOperation();
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
