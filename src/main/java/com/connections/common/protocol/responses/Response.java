package com.connections.common.protocol.responses;

import com.connections.common.protocol.responses.dto.DTO;

/**
 * Standard JSON response format from server to client.
 * 
 * @param <T> the type of data payload, which must implement DTO
 */
public class Response<T extends DTO> {
    private final Status status;
    private final String message;
    private final T data;

    /**
     * Private constructor to force use of static factory methods.
     */
    private Response(Status status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // --- FACTORY METHODS ---

    /**
     * Creates a successful response with data.
     * 
     * @param <T> the type of the data
     * @param message success message
     * @param data payload object (must implement DTO)
     * @return a success Response
     */
    public static <T extends DTO> Response<T> ok(String message, T data) {
        return new Response<>(Status.OK, message, data);
    }

    /**
     * Creates a successful response without data.
     * 
     * @param message success message
     * @return a success Response
     */
    public static Response<DTO> ok(String message) {
        return new Response<>(Status.OK, message, null);
    }

    /**
     * Creates an error response.
     * 
     * @param message error message
     * @return an error Response
     */
    public static Response<DTO> error(String message) {
        return new Response<>(Status.ERROR, message, null);
    }

    // --- GETTERS ---
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
