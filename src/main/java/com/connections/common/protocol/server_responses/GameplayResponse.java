package com.connections.common.protocol.server_responses;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.server_responses.dto.DTO;

/** Specialized response for active gameplay actions. */
public class GameplayResponse extends ServerResponse<DTO> {

    private GameplayResponse(Operation op, Status s, String m, DTO d) {
        super(op, s, m, d);
    }

    public static <T extends DTO> GameplayResponse ok(Operation op, String msg, T data) {
        return new GameplayResponse(op, Status.OK, msg, data);
    }

    public static <T extends DTO> GameplayResponse ok(Operation op, T data) {
        return new GameplayResponse(op, Status.OK, "Operation successful", data);
    }

    public static GameplayResponse error(Operation op, String msg) {
        return new GameplayResponse(op, Status.ERROR, msg, null);
    }

    public static GameplayResponse forbidden(Operation op, String msg) {
        return new GameplayResponse(op, Status.FORBIDDEN, msg, null);
    }
}
