package it.unipi.connections.common.protocol;

import it.unipi.connections.common.protocol.client_requests.*;
import it.unipi.connections.common.protocol.server_responses.*;
import it.unipi.connections.common.protocol.server_responses.dto.*;
import com.google.gson.annotations.SerializedName;

import java.util.EnumSet;

/**
 * Enum representing all protocol operations supported by the server. Each
 * operation specifies the
 * expected request, response, and (optionally) DTO classes.
 *
 * <p>
 * Ambiguities in protocol operations (such as distinguishing between current
 * and past puzzle
 * info/stats) are resolved using the
 * {@link Operation#resolve(String, com.google.gson.Gson)}
 * method. This method inspects the presence of the {@code gameId} field in the
 * incoming JSON
 * request to disambiguate between operations sharing the same serialized name.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> The internal {@code ProtocolMetadata} class uses
 * {@code Integer} (not
 * {@code int}) for the {@code gameId} field to allow for {@code null} values
 * during
 * deserialization. This is essential for distinguishing between requests that
 * do or do not specify
 * a {@code gameId}.
 * </p>
 */
public enum Operation {

    // -------------------------------------------------------------------------
    // USER MANAGEMENT
    // -------------------------------------------------------------------------

    @SerializedName("register")
    REGISTER(UserRequest.class, null),

    @SerializedName("login")
    LOGIN(UserRequest.class, null),

    @SerializedName("logout")
    LOGOUT(UserRequest.class, null),

    @SerializedName("updateCredentials")
    UPDATE_CREDENTIALS(UserRequest.class, null),

    // -------------------------------------------------------------------------
    // GAMEPLAY
    // -------------------------------------------------------------------------

    @SerializedName("submitProposal")
    SUBMIT_PROPOSAL(GameplayRequest.class, GuessResultDTO.class),

    /**
     * Request detailed info about the CURRENT puzzle. *
     * <p>
     * <strong>Note:</strong> Shares {@code @SerializedName("requestGameInfo")} with
     * {@link #GET_PAST_PUZZLE_INFO}. Use
     * {@link Operation#resolve(String, com.google.gson.Gson)} to
     * disambiguate based on the presence of {@code gameId} in the request.
     * </p>
     */
    @SerializedName("requestGameInfo")
    GET_PUZZLE_INFO(GameplayRequest.class, PuzzleStateDTO.class),

    /**
     * Request global stats for the CURRENT puzzle. *
     * <p>
     * <strong>Note:</strong> Shares {@code @SerializedName("requestGameStats")}
     * with
     * {@link #GET_PAST_PUZZLE_STATS}. Use
     * {@link Operation#resolve(String, com.google.gson.Gson)}
     * to disambiguate based on the presence of {@code gameId} in the request.
     * </p>
     */
    @SerializedName("requestGameStats")
    GET_PUZZLE_STATS(GameplayRequest.class, PuzzleStatsDTO.class),

    // -------------------------------------------------------------------------
    // HISTORY & STATS
    // -------------------------------------------------------------------------

    /**
     * Request detailed info about a PAST puzzle. *
     * <p>
     * <strong>Note:</strong> Shares {@code @SerializedName("requestGameInfo")} with
     * {@link #GET_PUZZLE_INFO}. Use
     * {@link Operation#resolve(String, com.google.gson.Gson)} to
     * disambiguate based on the presence of {@code gameId} in the request.
     * </p>
     */
    @SerializedName("requestGameInfo")
    GET_PAST_PUZZLE_INFO(HistoryRequest.class, PuzzleStateDTO.class),

    /**
     * Request global stats for a PAST puzzle. *
     * <p>
     * <strong>Note:</strong> Shares {@code @SerializedName("requestGameStats")}
     * with
     * {@link #GET_PUZZLE_STATS}. Use
     * {@link Operation#resolve(String, com.google.gson.Gson)} to
     * disambiguate based on the presence of {@code gameId} in the request.
     * </p>
     */
    @SerializedName("requestGameStats")
    GET_PAST_PUZZLE_STATS(HistoryRequest.class, PuzzleStatsDTO.class),

    @SerializedName("requestPlayerStats")
    GET_PLAYER_STATS(HistoryRequest.class, PlayerStatsDTO.class),

    @SerializedName("requestLeaderboard")
    GET_LEADERBOARD(HistoryRequest.class, LeaderboardDTO.class);

    // -------------------------------------------------------------------------
    // FIELDS & CONSTRUCTOR
    // -------------------------------------------------------------------------

    private final Class<? extends ClientRequest> requestClass;
    private final Class<? extends DTO> responseDtoClass;

    /**
     * Constructs an Operation enum constant.
     *
     * @param requestClass     the class of the request expected for this operation
     * @param responseDtoClass the DTO class expected in the server's response
     */
    Operation(Class<? extends ClientRequest> requestClass,
            Class<? extends DTO> responseDtoClass) {
        this.requestClass = requestClass;
        this.responseDtoClass = responseDtoClass;
    }

    public Class<? extends ClientRequest> getRequestClass() {
        return requestClass;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Class<? extends ServerResponse<?>> getResponseClass() {
        return (Class) ServerResponse.class;
    }

    public Class<? extends DTO> getResponseDtoClass() {
        return responseDtoClass;
    }

    // -------------------------------------------------------------------------
    // CATEGORY HELPERS
    // -------------------------------------------------------------------------

    public static EnumSet<Operation> userOps() {
        return EnumSet.of(REGISTER, LOGIN, LOGOUT, UPDATE_CREDENTIALS);
    }

    public static EnumSet<Operation> gameplayOps() {
        return EnumSet.of(SUBMIT_PROPOSAL, GET_PUZZLE_INFO, GET_PUZZLE_STATS);
    }

    public static EnumSet<Operation> historyOps() {
        return EnumSet.of(GET_PAST_PUZZLE_INFO, GET_PAST_PUZZLE_STATS, GET_PLAYER_STATS, GET_LEADERBOARD);
    }

    // -------------------------------------------------------------------------
    // RESOLUTION LOGIC
    // -------------------------------------------------------------------------

    /**
     * Resolves an Operation from a JSON request string, handling protocol-level
     * ambiguities.
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
     * Internal class used exclusively to extract the minimum metadata needed for
     * operation
     * resolution before full deserialization.
     */
    private static class ProtocolMetadata {
        Operation operation;

        @SerializedName("gameId")
        Integer gameId;
    }
}
