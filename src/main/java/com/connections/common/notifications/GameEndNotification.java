package com.connections.common.notifications;

import com.connections.common.dto.LeaderboardDTO;
import com.connections.common.dto.PuzzleStatsDTO;

/**
 * Notification sent to all participants when a puzzle ends.
 * Contains global information about the puzzle outcome.
 */
public class GameEndNotification implements Notification {
    private final String type = "game_end";
    private final LeaderboardDTO leaderboard;
    private final PuzzleStatsDTO stats;

    private GameEndNotification(LeaderboardDTO leaderboard, PuzzleStatsDTO stats) {
        this.leaderboard = leaderboard;
        this.stats = stats;
    }

    public static GameEndNotification create(LeaderboardDTO leaderboard, PuzzleStatsDTO stats) {
        return new GameEndNotification(leaderboard, stats);
    }

    @Override
    public String getType() {
        return type;
    }

    public LeaderboardDTO getLeaderboard() {
        return leaderboard;
    }

    public PuzzleStatsDTO getStats() {
        return stats;
    }
}
