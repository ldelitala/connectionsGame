package com.connections.common.notifications;

import com.connections.common.dto.LeaderboardDTO;
import com.connections.common.dto.GameStatsDTO;

/**
 * Notification sent to all participants when a game ends.
 * Contains global information about the game outcome.
 */
public class GameEndNotification implements Notification {
    private final String type = "game_end";
    private final LeaderboardDTO leaderboard;
    private final GameStatsDTO stats;

    private GameEndNotification(LeaderboardDTO leaderboard, GameStatsDTO stats) {
        this.leaderboard = leaderboard;
        this.stats = stats;
    }

    public static GameEndNotification create(LeaderboardDTO leaderboard, GameStatsDTO stats) {
        return new GameEndNotification(leaderboard, stats);
    }

    @Override
    public String getType() {
        return type;
    }

    public LeaderboardDTO getLeaderboard() {
        return leaderboard;
    }

    public GameStatsDTO getStats() {
        return stats;
    }
}
