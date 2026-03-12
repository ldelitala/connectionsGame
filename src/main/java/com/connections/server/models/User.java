package com.connections.server.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a registered user and their statistics.
 */
public class User {
    private String username;
    private String password;
    
    // Statistics
    private int puzzlesCompleted = 0;
    private int wins = 0;
    private int losses = 0;
    private int totalScore = 0;
    private int currentStreak = 0;
    private int maxStreak = 0;
    private int perfectPuzzles = 0; // 0 mistakes
    
    // Mistake histogram: key is number of mistakes (0 to 4), value is frequency
    // Special keys: 4 = lost by mistakes, 5 = DNF (Time out)
    private Map<Integer, Integer> mistakeHistogram = new HashMap<>();

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        for (int i = 0; i <= 5; i++) {
            mistakeHistogram.put(i, 0);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    // Statistic updates
    public void recordWin(int mistakes, int points) {
        puzzlesCompleted++;
        wins++;
        totalScore += points;
        currentStreak++;
        if (currentStreak > maxStreak) {
            maxStreak = currentStreak;
        }
        if (mistakes == 0) {
            perfectPuzzles++;
        }
        mistakeHistogram.put(mistakes, mistakeHistogram.getOrDefault(mistakes, 0) + 1);
    }

    public void recordLoss(int mistakes, int points) {
        puzzlesCompleted++;
        losses++;
        totalScore += points;
        currentStreak = 0;
        int key = (mistakes >= 4) ? 4 : 5;
        mistakeHistogram.put(key, mistakeHistogram.getOrDefault(key, 0) + 1);
    }

    public double getWinRate() {
        if (puzzlesCompleted == 0) return 0.0;
        return (double) wins / puzzlesCompleted * 100.0;
    }

    public double getAverageScore() {
        if (puzzlesCompleted == 0) return 0.0;
        return (double) totalScore / puzzlesCompleted;
    }

    public double getLossRate() {
        if (puzzlesCompleted == 0) return 0.0;
        return (double) losses / puzzlesCompleted * 100.0;
    }

    // Getters for stats
    public int getPuzzlesCompleted() { return puzzlesCompleted; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getTotalScore() { return totalScore; }
    public int getCurrentStreak() { return currentStreak; }
    public int getMaxStreak() { return maxStreak; }
    public int getPerfectPuzzles() { return perfectPuzzles; }
    public Map<Integer, Integer> getMistakeHistogram() { return mistakeHistogram; }
}
