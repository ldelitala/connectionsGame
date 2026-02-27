package com.connections.common;

import java.util.List;
import java.util.Map;

/**
 * Standard JSON response format from server to client.
 */
public class Response {
    private String status; // e.g., "ok", "error"
    private String message;
    private Object data; // Flexible field for payload (Game details, Stats, Leaderboard, etc.)

    public Response() {}

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public Response(String status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public static Response ok(String message) {
        return new Response("ok", message);
    }

    public static Response ok(String message, Object data) {
        return new Response("ok", message, data);
    }

    public static Response error(String message) {
        return new Response("error", message);
    }
}
