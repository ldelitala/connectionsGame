package com.connections.common.protocol.client_requests;

import com.connections.common.protocol.Operation;
import com.google.gson.annotations.SerializedName;

/**
 * Request subclass for historical and global statistics operations.
 * Handles past puzzle info/stats, player statistics, and leaderboard.
 */
public class HistoryRequest extends ClientRequest {
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

    /**
     * Creates a request for info about a past puzzle.
     */
    public static HistoryRequest getPuzzleInfo(int puzzleId) {
        return new HistoryRequest(Operation.GET_PUZZLE_INFO, puzzleId, null, null);
    }

    /**
     * Creates a request for stats about a past puzzle.
     */
    public static HistoryRequest getPuzzleStats(int puzzleId) {
        return new HistoryRequest(Operation.GET_PUZZLE_STATS, puzzleId, null, null);
    }

    /**
     * Creates a request for personal player statistics.
     */
    public static HistoryRequest getPlayerStats() {
        return new HistoryRequest(Operation.GET_PLAYER_STATS, null, null, null);
    }

    /**
     * Creates a request for the global leaderboard.
     * 
     * @param playerName optional player name to get rank for
     * @param topPlayers optional number of top players to retrieve
     */
    public static HistoryRequest getLeaderboard(String playerName, Integer topPlayers) {
        return new HistoryRequest(Operation.GET_LEADERBOARD, null, playerName, topPlayers);
    }

    public Integer getPuzzleId() { return puzzleId; }
    public String getPlayerName() { return playerName; }
    public Integer getTopPlayers() { return topPlayers; }
}
