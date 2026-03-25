package it.unipi.connections.common.protocol.client_requests;

import it.unipi.connections.common.protocol.Operation;

/**
 * Represents the abstract base class for all request messages sent from a
 * client to the server.
 * Provides the foundational structure to encapsulate a specific protocol
 * operation.
 */
public abstract class ClientRequest {

    private final Operation operation;

    protected ClientRequest(Operation operation) {
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }

    public boolean isValid() {
        return true;
    }
}
