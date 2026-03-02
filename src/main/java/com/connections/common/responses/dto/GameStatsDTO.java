package com.connections.common.responses.dto;

/**
 * DTO for global game statistics.
 */
public class GameStatsDTO {
    private final int gameId;
    private final boolean isCurrent;
    private final Long remainingTime;
    private final Integer inProgressCount;
    private final int finishedCount;
    private final int winnersCount;
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

    /** Factory method for active game stats. */
    public static GameStatsDTO current(int gameId, long remainingTime, int inProgress, int finished, int winners) {
        return new GameStatsDTO(gameId, true, remainingTime, inProgress, finished, winners, null, null);
    }

    /** Factory method for past game stats. */
    public static GameStatsDTO past(int gameId, int participants, int finished, int winners, double averageScore) {
        return new GameStatsDTO(gameId, false, null, null, finished, winners, participants, averageScore);
    }

    public int getGameId() { return gameId; }
    public boolean isCurrent() { return isCurrent; }
    public Long getRemainingTime() { return remainingTime; }
    public Integer getInProgressCount() { return inProgressCount; }
    public int getFinishedCount() { return finishedCount; }
    public int getWinnersCount() { return winnersCount; }
    public Integer getTotalParticipants() { return totalParticipants; }
    public Double getAverageScore() { return averageScore; }
}
