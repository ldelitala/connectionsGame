package com.connections.common.dto;

import java.util.Map;

public class PlayerStatsDTO {
    private final int puzzlesCompleted;
    private final double winRate;
    private final double lossRate;
    private final int currentStreak;
    private final int maxStreak;
    private final int perfectPuzzles;
    private final Map<Integer, Integer> mistakeHistogram;
    private final int totalScore;

    public PlayerStatsDTO(int puzzlesCompleted, double winRate, double lossRate, int currentStreak, 
                         int maxStreak, int perfectPuzzles, Map<Integer, Integer> mistakeHistogram, int totalScore) {
        this.puzzlesCompleted = puzzlesCompleted;
        this.winRate = winRate;
        this.lossRate = lossRate;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.perfectPuzzles = perfectPuzzles;
        this.mistakeHistogram = mistakeHistogram;
        this.totalScore = totalScore;
    }

    public int getPuzzlesCompleted() { return puzzlesCompleted; }
    public double getWinRate() { return winRate; }
    public double getLossRate() { return lossRate; }
    public int getCurrentStreak() { return currentStreak; }
    public int getMaxStreak() { return maxStreak; }
    public int getPerfectPuzzles() { return perfectPuzzles; }
    public Map<Integer, Integer> getMistakeHistogram() { return mistakeHistogram; }
    public int getTotalScore() { return totalScore; }
}
