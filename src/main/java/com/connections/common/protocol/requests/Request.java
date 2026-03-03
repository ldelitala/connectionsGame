package com.connections.common.protocol.requests;

import com.google.gson.annotations.SerializedName;

/**
 * Abstract base class for all client-server requests.
 * <p>
 * This class provides the foundational structure for requests sent from the client
 * to the server, primarily ensuring that every request includes an {@link Operation}.
 * Subclasses implement specific data fields required for different types of operations.
 * </p>
 */
public abstract class Request {

    /** The operation requested by the client. */
    @SerializedName("operation")
    private final Operation operation;

    /**
     * Protected constructor for subclasses to initialize the operation.
     * 
     * @param operation the operation type for this request
     */
    protected Request(Operation operation) {
        this.operation = operation;
    }

    /**
     * Gets the operation associated with this request.
     * 
     * @return the operation enum constant
     */
    public Operation getOperation() {
        return operation;
    }
}
