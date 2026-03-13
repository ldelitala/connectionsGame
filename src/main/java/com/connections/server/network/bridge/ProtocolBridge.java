package com.connections.server.network.bridge;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.server.network.ClientSession;
import com.connections.server.engine.GameEngine;
import com.google.gson.Gson;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Translates between raw network messages (JSON) and the GameEngine.
 * Performs serialization, deserialization, and session state updates synchronously.
 */
public class ProtocolBridge {

    private final GameEngine engine;
    private final Gson gson;

    public ProtocolBridge(GameEngine engine) {
        this.engine = engine;
        this.gson = new Gson();
    }

    /**
     * Processes a single JSON message and returns the serialized response.
     * Synchronous method to be called by a worker thread.
     */
    public ByteBuffer process(String json, ClientSession session) {
        ServerResponse<?> response;
        Operation op = null;
        ClientRequest request = null;

        try {
            // 1. Translation: JSON -> Java Objects
            op = Operation.resolve(json, gson);
            request = gson.fromJson(json, op.getRequestClass());

            // 2. Delegation: Core Logic
            response = engine.resolve(request, op, session.getUsername());

        } catch (Exception e) {
            response = ServerResponse.error(op != null ? op : Operation.REGISTER, 
                                           "Internal error: " + e.getMessage());
        }

        // 3. Session Management (State update on success)
        if (response.getStatus() == Status.OK && request != null) {
            updateSessionState(session, request, op);
        }

        // 4. Translation: Java Objects -> JSON -> ByteBuffer
        return serializeResponse(response);
    }

    public void handleDisconnect(String username) {
        engine.handleDisconnect(username);
    }

    private void updateSessionState(ClientSession session, ClientRequest request, Operation op) {
        switch (op) {
            case LOGIN:
                session.setUsername(((UserRequest) request).getUsername());
                break;
            case LOGOUT:
                session.setUsername(null);
                break;
            case UPDATE_CREDENTIALS:
                String newName = ((UserRequest) request).getNewName();
                if (newName != null && !newName.equals("-")) {
                    session.setUsername(newName);
                }
                break;
            default:
                break;
        }
    }

    private ByteBuffer serializeResponse(ServerResponse<?> response) {
        String json = gson.toJson(response) + "\n";
        return ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
    }
}
