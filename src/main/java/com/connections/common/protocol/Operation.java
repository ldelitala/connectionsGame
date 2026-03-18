package com.connections.common.protocol;

import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.common.protocol.server_responses.dto.*;
import com.google.gson.annotations.SerializedName;

/**
 * Enum mapping all network operations to their mandatory request and response structures. This is
 * the ultimate source of truth for the communication protocol.
 */
public enum Operation {

    // ------------------------------- USER MANAGEMENT ---------------------------------
    /** Register a new user account. */
    @SerializedName("register")
    REGISTER(UserRequest.class, ServerResponse.class, null),

    /** Authenticate a user and start a session. */
    @SerializedName("login")
    LOGIN(UserRequest.class, ServerResponse.class, null),

    /** Terminate the current user session. */
    @SerializedName("logout")
    LOGOUT(UserRequest.class, ServerResponse.class, null),

    /** Update the username or password of the current user. */
    @SerializedName("updateCredentials")
    UPDATE_CREDENTIALS(UserRequest.class, ServerResponse.class, null),



    // ------------------------------- GAMEPLAY ---------------------------------

    /** Submit a 4-word grouping proposal for the current puzzle. */
    @SerializedName("submitProposal")
    SUBMIT_PROPOSAL(GameplayRequest.class, ServerResponse.class, GuessResultDTO.class),

    /**
     * Request detailed info about the CURRENT puzzle. NOTE:
     * Shares @SerializedName("requestGameInfo") with GET_PAST_PUZZLE_INFO. Server resolves
     * ambiguity by checking the absence of 'gameId'.
     */
    @SerializedName("requestGameInfo")
    GET_PUZZLE_INFO(GameplayRequest.class, ServerResponse.class, PuzzleStateDTO.class),

    /**
     * Request global stats for the CURRENT puzzle. NOTE: Shares @SerializedName("requestGameStats")
     * with GET_PAST_PUZZLE_STATS. Server resolves ambiguity by checking the absence of 'gameId'.
     */
    @SerializedName("requestGameStats")
    GET_PUZZLE_STATS(GameplayRequest.class, ServerResponse.class, PuzzleStatsDTO.class),



    // ------------------------------- HISTORY & STATS ---------------------------------

    /**
     * Request detailed info about a PAST puzzle. NOTE: Shares @SerializedName("requestGameInfo")
     * with GET_PUZZLE_INFO. Use Operation.resolve(json, gson) to disambiguate based on the presence
     * of 'gameId' in the request.
     */
    @SerializedName("requestGameInfo")
    GET_PAST_PUZZLE_INFO(HistoryRequest.class, ServerResponse.class, PuzzleStateDTO.class),

    /**
     * Request global stats for a PAST puzzle. NOTE: Shares @SerializedName("requestGameStats") with
     * GET_PUZZLE_STATS. Use Operation.resolve(json, gson) to disambiguate based on the presence of
     * 'gameId' in the request.
     */
    @SerializedName("requestGameStats")
    GET_PAST_PUZZLE_STATS(HistoryRequest.class, ServerResponse.class, PuzzleStatsDTO.class),

    /** Request personal statistics for the logged-in user. */
    @SerializedName("requestPlayerStats")
    GET_PLAYER_STATS(HistoryRequest.class, ServerResponse.class, PlayerStatsDTO.class),

    /** Request the global leaderboard or a specific player's rank. */
    @SerializedName("requestLeaderboard")
    GET_LEADERBOARD(HistoryRequest.class, ServerResponse.class, LeaderboardDTO.class);



    /// FIELDS & CONSTRUCTOR

    private final Class<? extends ClientRequest> requestClass;
    private final Class<? extends ServerResponse> responseClass;
    private final Class<? extends DTO> responseDtoClass;

    Operation(Class<? extends ClientRequest> requestClass,
            Class<? extends ServerResponse> responseClass,
            Class<? extends DTO> responseDtoClass) {
        this.requestClass = requestClass;
        this.responseClass = responseClass;
        this.responseDtoClass = responseDtoClass;
    }

    /** @return the class of the request expected for this operation. */
    public Class<? extends ClientRequest> getRequestClass() {
        return requestClass;
    }

    /** @return the specialized ServerResponse subclass for this operation. */
    public Class<? extends ServerResponse> getResponseClass() {
        return responseClass;
    }

    /** @return the DTO class expected in the server's response for this operation. */
    public Class<? extends DTO> getResponseDtoClass() {
        return responseDtoClass;
    }

    /**
     * Resolves an Operation from a JSON request string, handling protocol-level ambiguities.
     *
     * @param json the raw JSON request
     * @param gson the Gson instance to use for parsing
     * @return the resolved Operation
     * @throws IllegalArgumentException if the operation is unknown or missing
     */
    public static Operation resolve(String json, com.google.gson.Gson gson) {
        ProtocolMetadata metadata = gson.fromJson(json, ProtocolMetadata.class);

        if (metadata == null || metadata.operation == null) {
            throw new IllegalArgumentException("Unknown or missing operation");
        }

        Operation op = metadata.operation;

        // Ambiguity resolution for info/stats
        if (metadata.gameId != null) {
            // Presence of gameId -> Force PAST version if currently CURRENT
            if (op == Operation.GET_PUZZLE_INFO) {
                return Operation.GET_PAST_PUZZLE_INFO;
            }
            if (op == Operation.GET_PUZZLE_STATS) {
                return Operation.GET_PAST_PUZZLE_STATS;
            }
        } else {
            // Absence of gameId -> Force CURRENT version if currently PAST
            if (op == Operation.GET_PAST_PUZZLE_INFO) {
                return Operation.GET_PUZZLE_INFO;
            }
            if (op == Operation.GET_PAST_PUZZLE_STATS) {
                return Operation.GET_PUZZLE_STATS;
            }
        }

        return op;
    }

    /**
     * Internal class to extract minimum metadata needed for operation resolution.
     */
    private static class ProtocolMetadata {
        Operation operation;
        @SerializedName("gameId")
        Integer gameId;
    }
}
