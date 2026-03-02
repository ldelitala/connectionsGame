package com.connections.common.dto;

public class LeaderboardEntryDTO {
    private final int rank;
    private final String username;
    private final int score;

    public LeaderboardEntryDTO(int rank, String username, int score) {
        this.rank = rank;
        this.username = username;
        this.score = score;
    }

    public int getRank() { return rank; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
}
