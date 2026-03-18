package com.connections.common.protocol.server_responses.dto;

/**
 * DTO representing a single entry in the leaderboard. Contains the player's rank, username, and
 * total score. This DTO is used as part of the LeaderboardDTO to provide detailed information about
 * each player on the leaderboard. The 'rank' field indicates the player's position on the
 * leaderboard (1 for first place, 2 for second, etc.), while 'score' represents their total points.
 */
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

    public int getRank() {
        return rank;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }
}
