package com.connections.common.protocol.client_requests;

import com.google.gson.annotations.SerializedName;

/**
 * Request subclass for historical and global statistics operations.
 * Handles past game info/stats, player statistics, and leaderboard.
 */
public class HistoryRequest extends ClientRequest {
    private final Integer gameId;
    
    @SerializedName("playerName")
    private final String playerName;
    
    @SerializedName("topPlayers")
    private final Integer topPlayers;

    private HistoryRequest(Operation op, Integer gameId, String playerName, Integer topPlayers) {
        super(op);
        this.gameId = gameId;
        this.playerName = playerName;
        this.topPlayers = topPlayers;
    }

    /**
     * Creates a request for info about a past game.
     */
    public static HistoryRequest getGameInfo(int gameId) {
        return new HistoryRequest(Operation.GET_GAME_INFO, gameId, null, null);
    }

    /**
     * Creates a request for stats about a past game.
     */
    public static HistoryRequest getGameStats(int gameId) {
        return new HistoryRequest(Operation.GET_GAME_STATS, gameId, null, null);
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

    public Integer getGameId() { return gameId; }
    public String getPlayerName() { return playerName; }
    public Integer getTopPlayers() { return topPlayers; }
}
