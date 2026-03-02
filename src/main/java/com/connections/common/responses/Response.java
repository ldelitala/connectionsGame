package com.connections.common.responses;

/**
 * Standard JSON response format from server to client.
 */
public class Response {
    private final Status status;
    private final String message;
    private final Object data;

    /**
     * Private constructor to force use of static factory methods.
     */
    private Response(Status status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // --- FACTORY METHODS ---

    /**
     * Creates a successful response with data.
     * 
     * @param message success message
     * @param data payload object (usually a DTO)
     * @return a success Response
     */
    public static Response ok(String message, Object data) {
        return new Response(Status.OK, message, data);
    }

    /**
     * Creates a successful response without data.
     * 
     * @param message success message
     * @return a success Response
     */
    public static Response ok(String message) {
        return new Response(Status.OK, message, null);
    }

    /**
     * Creates an error response.
     * 
     * @param message error message
     * @return an error Response
     */
    public static Response error(String message) {
        return new Response(Status.ERROR, message, null);
    }

    // --- GETTERS ---
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
