package com.connections.common.protocol.server_responses.dto;

public class LeaderboardEntryDTO implements DTO {
    private final int rank;
    private final String username;
    private final int score;

    private LeaderboardEntryDTO(int rank, String username, int score) {
        this.rank = rank;
        this.username = username;
        this.score = score;
    }

    public static LeaderboardEntryDTO create(int rank, String username, int score) {
        return new LeaderboardEntryDTO(rank, username, score);
    }

    public int getRank() { return rank; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
}
