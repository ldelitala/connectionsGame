package com.connections.common.dto;

/**
 * DTO for global game statistics (both active and concluded).
 */
public class GameStatsDTO implements DTO {
    // Common fields
    private final int gameId;
    private final boolean isCurrent;
    private final int finishedCount;
    private final int winnersCount;

    // Fields for current game
    private final Long remainingTime;
    private final Integer inProgressCount;

    // Fields for past game
    private final Integer totalParticipants;
    private final Double averageScore;

    private GameStatsDTO(int gameId, boolean isCurrent, Long remainingTime, Integer inProgressCount, 
                        int finishedCount, int winnersCount, Integer totalParticipants, Double averageScore) {
        this.gameId = gameId;
        this.isCurrent = isCurrent;
        this.remainingTime = remainingTime;
        this.inProgressCount = inProgressCount;
        this.finishedCount = finishedCount;
        this.winnersCount = winnersCount;
        this.totalParticipants = totalParticipants;
        this.averageScore = averageScore;
    }

    /**
     * Factory method for active game stats.
     * 
     * @param gameId ID of the game
     * @param remainingTime time left in milliseconds
     * @param inProgress number of players still playing
     * @param finished number of players who completed the game
     * @param winners number of players who won
     * @return a new GameStatsDTO for the current game
     */
    public static GameStatsDTO current(int gameId, long remainingTime, int inProgress, int finished, int winners) {
        return new GameStatsDTO(gameId, true, remainingTime, inProgress, finished, winners, null, null);
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
    public static GameStatsDTO past(int gameId, int participants, int finished, int winners, double averageScore) {
        return new GameStatsDTO(gameId, false, null, null, finished, winners, participants, averageScore);
    }

    // --- GETTERS ---
    public int getGameId() { return gameId; }
    public boolean isCurrent() { return isCurrent; }
    public Long getRemainingTime() { return remainingTime; }
    public Integer getInProgressCount() { return inProgressCount; }
    public int getFinishedCount() { return finishedCount; }
    public int getWinnersCount() { return winnersCount; }
    public Integer getTotalParticipants() { return totalParticipants; }
    public Double getAverageScore() { return averageScore; }
}
