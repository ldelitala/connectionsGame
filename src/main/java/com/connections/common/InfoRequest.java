package com.connections.common;

/**
 * specialized Request subclass for information-retrieval operations.
 * <p>
 * This class handles requests for personal statistics, game info, global game stats,
 * and the leaderboard. It uses static factory methods to simplify request creation.
 * </p>
 */
public class InfoRequest extends Request {

    /** Username for specific player rank requests. */
    private String playerName;
    /** Number of top players to retrieve for leaderboard requests. */
    private Integer topPlayers;

    /**
     * Private constructor to force use of static factory methods.
     * 
     * @param operation the specific information operation
     */
    private InfoRequest(Operation operation) {
        super(operation);
    }

    // --- FACTORY METHODS ---

    /**
     * Creates a request for the logged-in user's personal statistics.
     * 
     * @return a new InfoRequest for player stats
     */
    public static InfoRequest requestPlayerStats() {
        return new InfoRequest(Operation.REQUEST_PLAYER_STATS);
    }

    /**
     * Creates a request for the leaderboard.
     * 
     * @param playerName optional player name to get a specific rank (can be null)
     * @param topPlayers optional number of top players to retrieve (can be null)
     * @return a new InfoRequest for the leaderboard
     */
    public static InfoRequest requestLeaderboard(String playerName, Integer topPlayers) {
        InfoRequest req = new InfoRequest(Operation.REQUEST_LEADERBOARD);
        req.playerName = playerName;
        req.topPlayers = topPlayers;
        return req;
    }

    // --- GETTERS ---
    public String getPlayerName() { return playerName; }
    public Integer getTopPlayers() { return topPlayers; }
}
