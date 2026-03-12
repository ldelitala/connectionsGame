package com.connections.common.protocol.client_requests;

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
