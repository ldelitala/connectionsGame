package com.connections.common.protocol.server_responses;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.server_responses.dto.DTO;

/**
 * Base class for all server responses.
 * Includes the operation ID to ensure responses are self-describing.
 * 
 * @param <T> The type of DTO contained in the response.
 */
public class ServerResponse<T extends DTO> {
    
    private final Operation operation;
    private final Status status;
    private final String message;
    private final T data;

    public ServerResponse(Operation operation, Status status, String message, T data) {
        this.operation = operation;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public Operation getOperation() { return operation; }
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public T getData() { return data; }

    // --- UNIVERSAL FACTORY METHODS ---

    /** Creates a successful response with a generic DTO. */
    public static <D extends DTO> ServerResponse<D> ok(Operation op, String msg, D data) {
        return new ServerResponse<>(op, Status.OK, msg, data);
    }

    /** Creates a successful response with a default success message. */
    public static <D extends DTO> ServerResponse<D> ok(Operation op, D data) {
        return new ServerResponse<>(op, Status.OK, "Operation successful", data);
    }

    /** Creates a successful response with NO data (null DTO). */
    public static ServerResponse<DTO> ok(Operation op, String msg) {
        return new ServerResponse<>(op, Status.OK, msg, null);
    }

    /** Creates an error response with a custom message. */
    public static <D extends DTO> ServerResponse<D> error(Operation op, String msg) {
        return new ServerResponse<>(op, Status.ERROR, msg, null);
    }

    /** Creates a forbidden (unauthorized) response with a custom message. */
    public static <D extends DTO> ServerResponse<D> forbidden(Operation op, String msg) {
        return new ServerResponse<>(op, Status.FORBIDDEN, msg, null);
    }
}
