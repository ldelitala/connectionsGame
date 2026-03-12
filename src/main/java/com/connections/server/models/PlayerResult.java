package com.connections.server.models;

/**
 * Represents the internal state of a player's performance in a puzzle. Internal to the server, NOT
 * shared with the client.
 */
public class PlayerResult {
    private final int mistakes;
    private final int correctGroups;
    private final int points;
    private final boolean won;
    private final boolean finished;

    public PlayerResult(int mistakes, int correctGroups, boolean finished) {
        this.mistakes = mistakes;
        this.correctGroups = correctGroups;
        this.finished = finished;
        // 3 correct groups = win (4th is implicit)
        this.won = (correctGroups == 3);
        this.points = calculatePoints(correctGroups, mistakes);
    }

    private int calculatePoints(int correctGroups, int mistakes) {
        int basePoints = 6 * correctGroups;
        return basePoints - (mistakes * 4);
    }

    public int getMistakes() {
        return mistakes;
    }

    public int getCorrectGroups() {
        return correctGroups;
    }

    public int getPoints() {
        return points;
    }

    public boolean isWon() {
        return won;
    }

    public boolean isFinished() {
        return finished;
    }
}
