package com.connections.common;

/**
 * Implementation for Information Requests (requestGameInfo, requestGameStats, requestPlayerStats, requestLeaderboard).
 */
public class InfoRequest extends Request {
    private final Integer gameId;
    private final String playerName;
    private final Integer topPlayers;

    public InfoRequest(Operation op, Integer gameId, String playerName, Integer topPlayers) {
        super(op);
        this.gameId = gameId;
        this.playerName = playerName;
        this.topPlayers = topPlayers;
    }

    public Integer getGameId() { return gameId; }
    public String getPlayerName() { return playerName; }
    public Integer getTopPlayers() { return topPlayers; }
}
