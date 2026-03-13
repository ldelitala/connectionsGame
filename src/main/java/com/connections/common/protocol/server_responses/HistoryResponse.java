package com.connections.common.protocol.server_responses;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.server_responses.dto.DTO;

/** Specialized response for history, stats and leaderboard requests. */
public class HistoryResponse extends ServerResponse<DTO> {

    private HistoryResponse(Operation op, Status s, String m, DTO d) {
        super(op, s, m, d);
    }

    public static <T extends DTO> HistoryResponse ok(Operation op, T data) {
        return new HistoryResponse(op, Status.OK, "History data retrieved", data);
    }

    public static <T extends DTO> HistoryResponse ok(Operation op, String msg, T data) {
        return new HistoryResponse(op, Status.OK, msg, data);
    }

    public static HistoryResponse error(Operation op, String msg) {
        return new HistoryResponse(op, Status.ERROR, msg, null);
    }

    public static HistoryResponse forbidden(Operation op, String msg) {
        return new HistoryResponse(op, Status.FORBIDDEN, msg, null);
    }
}
