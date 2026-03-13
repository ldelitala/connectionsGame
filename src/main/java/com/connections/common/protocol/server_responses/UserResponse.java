package com.connections.common.protocol.server_responses;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.server_responses.dto.DTO;

/** Specialized response for User account operations. */
public class UserResponse extends ServerResponse<DTO> {

    private UserResponse(Operation op, Status s, String m, DTO d) {
        super(op, s, m, d);
    }

    public static UserResponse ok(Operation op, String msg) {
        return new UserResponse(op, Status.OK, msg, null);
    }

    public static UserResponse error(Operation op, String msg) {
        return new UserResponse(op, Status.ERROR, msg, null);
    }

    public static UserResponse forbidden(Operation op, String msg) {
        return new UserResponse(op, Status.FORBIDDEN, msg, null);
    }
}
