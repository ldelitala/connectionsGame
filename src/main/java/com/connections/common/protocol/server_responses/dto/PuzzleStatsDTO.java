package com.connections.common.protocol.server_responses.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for global puzzle statistics (both active and concluded). Follows the project's requirements
 * for what info to return for active vs finished games. Uses factory methods to create instances
 * for active vs concluded puzzles, ensuring that only relevant fields are populated for each case.
 */
public class PuzzleStatsDTO implements DTO {
    @SerializedName("gameId")
    private final int puzzleId;
    private final boolean finished;

    // Active puzzle fields
    private final Long remainingTime;
    private final Integer activePlayers; // Still playing
    private final Integer finishedPlayers; // Finished (win or loss)
    private final Integer wonPlayers; // Finished with win

    // Concluded puzzle fields
    private final Integer totalParticipants;
    private final Double averageScore;

    private PuzzleStatsDTO(int puzzleId, boolean finished, Long remainingTime,
            Integer activePlayers, Integer finishedPlayers, Integer wonPlayers,
            Integer totalParticipants, Double averageScore) {
        this.puzzleId = puzzleId;
        this.finished = finished;
        this.remainingTime = remainingTime;
        this.activePlayers = activePlayers;
        this.finishedPlayers = finishedPlayers;
        this.wonPlayers = wonPlayers;
        this.totalParticipants = totalParticipants;
        this.averageScore = averageScore;
    }

    /**
     * Factory method for active puzzle stats.
     * 
     * @param puzzleId ID of the puzzle
     * @param remainingTime time left in milliseconds
     * @param active number of players currently playing
     * @param finished number of players who completed the puzzle
     * @param winners number of players who won
     * @return a new PuzzleStatsDTO for the active puzzle
     */
    public static PuzzleStatsDTO active(int puzzleId, long remainingTime, int active, int finished,
            int winners) {
        return new PuzzleStatsDTO(puzzleId, false, remainingTime, active, finished, winners, null,
                null);
    }

    /**
     * Factory method for concluded puzzle stats.
     * 
     * @param puzzleId ID of the puzzle
     * @param participants total number of players who participated
     * @param finished number of players who completed the puzzle
     * @param winners number of players who won
     * @param averageScore average score of all participants
     * @return a new PuzzleStatsDTO for a past puzzle
     */
    public static PuzzleStatsDTO concluded(int puzzleId, int participants, int finished,
            int winners, double averageScore) {
        return new PuzzleStatsDTO(puzzleId, true, null, null, finished, winners, participants,
                averageScore);
    }

    // --- GETTERS ---
    public int getPuzzleId() {
        return puzzleId;
    }

    public boolean isFinished() {
        return finished;
    }

    public Long getRemainingTime() {
        return remainingTime;
    }

    public Integer getActivePlayers() {
        return activePlayers;
    }

    public Integer getFinishedPlayers() {
        return finishedPlayers;
    }

    public Integer getWonPlayers() {
        return wonPlayers;
    }

    public Integer getTotalParticipants() {
        return totalParticipants;
    }

    public Double getAverageScore() {
        return averageScore;
    }
}
