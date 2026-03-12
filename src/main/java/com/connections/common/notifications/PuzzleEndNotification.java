package com.connections.common.notifications;

import com.connections.common.dto.LeaderboardDTO;
import com.connections.common.dto.PuzzleStatsDTO;

/**
 * Notification sent to all participants when a puzzle ends.
 */
public class PuzzleEndNotification extends Notification {

    /** The unique identifier for this notification type. */
    public static final String TYPE_ID = "game_end";

    private final LeaderboardDTO leaderboard;
    private final PuzzleStatsDTO stats;

    private PuzzleEndNotification(LeaderboardDTO leaderboard, PuzzleStatsDTO stats) {
        super(TYPE_ID);
        this.leaderboard = leaderboard;
        this.stats = stats;
    }

    public static PuzzleEndNotification create(LeaderboardDTO leaderboard, PuzzleStatsDTO stats) {
        return new PuzzleEndNotification(leaderboard, stats);
    }

    public LeaderboardDTO getLeaderboard() {
        return leaderboard;
    }

    public PuzzleStatsDTO getStats() {
        return stats;
    }
}
