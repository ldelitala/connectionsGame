package it.unipi.connections.common.protocol.server_responses.dto;

import java.util.Map;

/**
 * Data Transfer Object representing a player's statistics. Includes completed puzzles, win/loss
 * rates (as percentages), current and maximum winning streaks, count of perfect puzzles, mistake
 * distribution (histogram), total score, and average score per puzzle. Used for server responses to
 * provide detailed player performance metrics. The average score is automatically calculated;
 * win/loss rates are expressed as values between 0 and 1.
 */
public class PlayerStatsDTO implements DTO {
    private final int puzzlesCompleted;
    private final double winRate;
    private final double lossRate;
    private final int currentStreak;
    private final int maxStreak;
    private final int perfectPuzzles;
    private final Map<Integer, Integer> mistakeHistogram;
    private final int totalScore;
    private final double averageScore;

    private PlayerStatsDTO(int puzzlesCompleted, double winRate, double lossRate, int currentStreak,
            int maxStreak, int perfectPuzzles, Map<Integer, Integer> mistakeHistogram,
            int totalScore) {
        this.puzzlesCompleted = puzzlesCompleted;
        this.winRate = winRate;
        this.lossRate = lossRate;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.perfectPuzzles = perfectPuzzles;
        this.mistakeHistogram = mistakeHistogram;
        this.totalScore = totalScore;
        // Automatically calculate average score
        this.averageScore = (puzzlesCompleted > 0) ? (double) totalScore / puzzlesCompleted : 0.0;
    }

    /**
     * Factory method to create player stats.
     */
    public static PlayerStatsDTO create(int puzzles, double winR, double lossR, int currS, int maxS,
            int perfect, Map<Integer, Integer> hist, int score) {
        return new PlayerStatsDTO(puzzles, winR, lossR, currS, maxS, perfect, hist, score);
    }

    public int getPuzzlesCompleted() {
        return puzzlesCompleted;
    }

    public double getWinRate() {
        return winRate;
    }

    public double getLossRate() {
        return lossRate;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getMaxStreak() {
        return maxStreak;
    }

    public int getPerfectPuzzles() {
        return perfectPuzzles;
    }

    public Map<Integer, Integer> getMistakeHistogram() {
        return mistakeHistogram;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public double getAverageScore() {
        return averageScore;
    }
}
