package com.connections.common.protocol.server_responses;

import com.connections.common.dto.DTO;
import java.io.Serializable;

/**
 * Generic response structure for all messages sent from the server to the client.
 * 
 * @param <T> The type of DTO contained in the response.
 */
public class ServerResponse<T extends DTO> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Status status;
    private final String message;
    private final T data;

    public ServerResponse(Status status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T extends DTO> ServerResponse<T> ok(String message, T data) {
        return new ServerResponse<>(Status.OK, message, data);
    }

    public static ServerResponse<DTO> ok(String message) {
        return new ServerResponse<>(Status.OK, message, null);
    }

    public static ServerResponse<DTO> error(String message) {
        return new ServerResponse<>(Status.ERROR, message, null);
    }

    public static ServerResponse<DTO> forbidden(String message) {
        return new ServerResponse<>(Status.FORBIDDEN, message, null);
    }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
