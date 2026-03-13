package com.connections.common.protocol.server_responses;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.server_responses.dto.DTO;

/**
 * Base abstract class for all server responses.
 * Includes the operation ID to ensure responses are self-describing.
 * 
 * @param <T> The type of DTO contained in the response.
 */
public abstract class ServerResponse<T extends DTO> {
    
    private final Operation operation;
    private final Status status;
    private final String message;
    private final T data;

    protected ServerResponse(Operation operation, Status status, String message, T data) {
        this.operation = operation;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public Operation getOperation() { return operation; }
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
