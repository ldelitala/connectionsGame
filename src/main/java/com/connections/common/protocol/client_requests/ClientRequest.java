package com.connections.common.protocol.client_requests;

import java.io.Serializable;

/**
 * Base class for all requests sent from the client to the server.
 */
public abstract class ClientRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
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
