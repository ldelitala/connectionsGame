package com.connections.common.protocol.client_requests;

import com.connections.common.protocol.Operation;

/**
 * Base class for all requests sent from the client to the server.
 */
public abstract class ClientRequest {
    
    private final Operation operation;

    protected ClientRequest(Operation operation) {
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }

    /**
     * Validates the request parameters.
     * Default implementation returns true.
     */
    public boolean isValid() {
        return true;
    }
}
