package it.unipi.connections.common.protocol.client_requests;

import it.unipi.connections.common.protocol.Operation;
import com.google.gson.annotations.SerializedName;
import java.util.EnumSet;

/**
 * Request subclass for historical and global statistics operations.
 * Handles past puzzle info/stats, player statistics, and leaderboard.
 */
public class HistoryRequest extends ClientRequest {

    private static final EnumSet<Operation> ALLOWED_OPERATIONS = Operation.historyOps();

    @SerializedName("gameId")
    private final Integer puzzleId;

    @SerializedName("playerName")
    private final String playerName;

    @SerializedName("topPlayers")
    private final Integer topPlayers;

    private HistoryRequest(Operation op, Integer puzzleId, String playerName, Integer topPlayers) {
        super(op);
        this.puzzleId = puzzleId;
        this.playerName = playerName;
        this.topPlayers = topPlayers;
    }

    // --------------------------------------------------------------------------
    // Factory Methods
    // --------------------------------------------------------------------------

    public static HistoryRequest getPuzzleInfo(int puzzleId) {
        return new HistoryRequest(Operation.GET_PAST_PUZZLE_INFO, puzzleId, null, null);
    }

    public static HistoryRequest getPuzzleStats(int puzzleId) {
        return new HistoryRequest(Operation.GET_PAST_PUZZLE_STATS, puzzleId, null, null);
    }

    public static HistoryRequest getPlayerStats() {
        return new HistoryRequest(Operation.GET_PLAYER_STATS, null, null, null);
    }

    /**
     * Creates a request for the entire global leaderboard.
     */
    public static HistoryRequest getLeaderboard() {
        return new HistoryRequest(Operation.GET_LEADERBOARD, null, null, null);
    }

    /**
     * Creates a request for the top N players in the leaderboard.
     */
    public static HistoryRequest getLeaderboard(int topPlayers) {
        return new HistoryRequest(Operation.GET_LEADERBOARD, null, null, topPlayers);
    }

    /**
     * Creates a request for a specific player's rank in the leaderboard.
     */
    public static HistoryRequest getLeaderboard(String playerName) {
        return new HistoryRequest(Operation.GET_LEADERBOARD, null, playerName, null);
    }

    // --------------------------------------------------------------------------

    // --------------------------------------------------------------------------
    // Getters
    // --------------------------------------------------------------------------

    public Integer getPuzzleId() {
        return puzzleId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Integer getTopPlayers() {
        return topPlayers;
    }

    // --------------------------------------------------------------------------

    @Override
    public boolean isValid() {
        Operation op = getOperation();
        if (!ALLOWED_OPERATIONS.contains(op)) {
            return false;
        }

        if (op == Operation.GET_PAST_PUZZLE_INFO || op == Operation.GET_PAST_PUZZLE_STATS) {
            return puzzleId != null;
        }

        if (op == Operation.GET_LEADERBOARD) {
            return !(playerName != null && topPlayers != null); // Can't specify both playerName and topPlayers
        }

        return true;
    }
}
