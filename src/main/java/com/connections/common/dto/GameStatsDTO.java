package com.connections.common.dto;

/**
 * DTO for global game statistics (both active and concluded).
 * Follows the project's requirements for what info to return for active vs finished games.
 */
public class GameStatsDTO implements DTO {
    private final int gameId;
    private final boolean finished;

    // Active game fields
    private final Long remainingTime;
    private final Integer activePlayers; // Still playing
    private final Integer finishedPlayers; // Finished (win or loss)
    private final Integer wonPlayers; // Finished with win

    // Concluded game fields
    private final Integer totalParticipants;
    private final Double averageScore;

    private GameStatsDTO(int gameId, boolean finished, Long remainingTime,
            Integer activePlayers, Integer finishedPlayers, Integer wonPlayers,
            Integer totalParticipants, Double averageScore) {
        this.gameId = gameId;
        this.finished = finished;
        this.remainingTime = remainingTime;
        this.activePlayers = activePlayers;
        this.finishedPlayers = finishedPlayers;
        this.wonPlayers = wonPlayers;
        this.totalParticipants = totalParticipants;
        this.averageScore = averageScore;
    }

    /**
     * Factory method for active game stats.
     * 
     * @param gameId ID of the game
     * @param remainingTime time left in milliseconds
     * @param active number of players currently playing
     * @param finished number of players who completed the game
     * @param winners number of players who won
     * @return a new GameStatsDTO for the active game
     */
    public static GameStatsDTO active(int gameId, long remainingTime, int active, int finished, int winners) {
        return new GameStatsDTO(gameId, false, remainingTime, active, finished, winners, null, null);
    }

    /**
     * Factory method for concluded game stats.
     * 
     * @param gameId ID of the game
     * @param participants total number of players who participated
     * @param finished number of players who completed the game
     * @param winners number of players who won
     * @param averageScore average score of all participants
     * @return a new GameStatsDTO for a past game
     */
    public static GameStatsDTO concluded(int gameId, int participants, int finished, int winners, double averageScore) {
        return new GameStatsDTO(gameId, true, null, null, finished, winners, participants, averageScore);
    }

    // --- GETTERS ---
    public int getGameId() { return gameId; }
    public boolean isFinished() { return finished; }
    public Long getRemainingTime() { return remainingTime; }
    public Integer getActivePlayers() { return activePlayers; }
    public Integer getFinishedPlayers() { return finishedPlayers; }
    public Integer getWonPlayers() { return wonPlayers; }
    public Integer getTotalParticipants() { return totalParticipants; }
    public Double getAverageScore() { return averageScore; }
}
