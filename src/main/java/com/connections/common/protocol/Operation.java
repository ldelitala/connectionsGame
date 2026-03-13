package com.connections.common.protocol;

import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.common.protocol.server_responses.dto.*;
import com.google.gson.annotations.SerializedName;

/**
 * Enum mapping all network operations to their mandatory request and response structures.
 * This is the ultimate source of truth for the communication protocol.
 */
public enum Operation {
    
    /** Register a new user account. */
    @SerializedName("register")
    REGISTER(UserRequest.class, UserResponse.class, null),

    /** Authenticate a user and start a session. */
    @SerializedName("login")
    LOGIN(UserRequest.class, UserResponse.class, null),

    /** Terminate the current user session. */
    @SerializedName("logout")
    LOGOUT(UserRequest.class, UserResponse.class, null),

    /** Update the username or password of the current user. */
    @SerializedName("updateCredentials")
    UPDATE_CREDENTIALS(UserRequest.class, UserResponse.class, null),

    /** Submit a 4-word grouping proposal for the current puzzle. */
    @SerializedName("submitProposal")
    SUBMIT_PROPOSAL(GameplayRequest.class, GameplayResponse.class, GuessResultDTO.class),

    /** Request detailed information about a specific puzzle or the current puzzle. 
     *  Server identifies type by checking 'gameId' field: null/missing -> GameplayRequest, present -> HistoryRequest. */
    @SerializedName("requestGameInfo")
    GET_PUZZLE_INFO(null, GameplayResponse.class, PuzzleStateDTO.class),

    /** Request global statistics for a specific puzzle. 
     *  Server identifies type by checking 'gameId' field: null/missing -> GameplayRequest, present -> HistoryRequest. */
    @SerializedName("requestGameStats")
    GET_PUZZLE_STATS(null, GameplayResponse.class, PuzzleStatsDTO.class),

    /** Request personal statistics for the logged-in user. */
    @SerializedName("requestPlayerStats")
    GET_PLAYER_STATS(HistoryRequest.class, HistoryResponse.class, PlayerStatsDTO.class),

    /** Request the global leaderboard or a specific player's rank. */
    @SerializedName("requestLeaderboard")
    GET_LEADERBOARD(HistoryRequest.class, HistoryResponse.class, LeaderboardDTO.class);

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
}
