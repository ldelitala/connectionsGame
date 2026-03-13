package com.connections.common.protocol.client_requests;

import com.connections.common.protocol.server_responses.dto.*;
import com.google.gson.annotations.SerializedName;

/**
 * Enum representing all possible operations the client can request from the server.
 * <p>
 * This enum is used to identify the type of request and ensures consistent communication between
 * the client and the server. It uses Gson's {@code @SerializedName} to map the enum constants to
 * specific JSON string values.
 * </p>
 */
public enum Operation {
    /** Register a new user account. */
    @SerializedName("register")
    REGISTER(null),

    /** Authenticate a user and start a session. */
    @SerializedName("login")
    LOGIN(null),

    /** Terminate the current user session. */
    @SerializedName("logout")
    LOGOUT(null),

    /** Update the username or password of the current user. */
    @SerializedName("updateCredentials")
    UPDATE_CREDENTIALS(null),

    /** Submit a 4-word grouping proposal for the current puzzle. */
    @SerializedName("submitProposal")
    SUBMIT_PROPOSAL(GuessResultDTO.class),

    /** Request detailed information about a specific puzzle or the current puzzle. */
    @SerializedName("requestGameInfo")
    GET_PUZZLE_INFO(PuzzleStateDTO.class),

    /** Request global statistics for a specific puzzle. */
    @SerializedName("requestGameStats")
    GET_PUZZLE_STATS(PuzzleStatsDTO.class),

    /** Request personal statistics for the logged-in user. */
    @SerializedName("requestPlayerStats")
    GET_PLAYER_STATS(PlayerStatsDTO.class),

    /** Request the global leaderboard or a specific player's rank. */
    @SerializedName("requestLeaderboard")
    GET_LEADERBOARD(LeaderboardDTO.class);

    private final Class<? extends DTO> responseDtoClass;

    Operation(Class<? extends DTO> responseDtoClass) {
        this.responseDtoClass = responseDtoClass;
    }

    /** @return the DTO class expected in the server's response for this operation. */
    public Class<? extends DTO> getResponseDtoClass() {
        return responseDtoClass;
    }
}
