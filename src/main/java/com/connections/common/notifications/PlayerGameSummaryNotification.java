package com.connections.common.notifications;

import com.connections.common.dto.PuzzleStateDTO;

/**
 * Notification sent privately to a specific player when their puzzle ends.
 */
public class PlayerGameSummaryNotification extends Notification {
    private final PuzzleStateDTO personalInfo;

    private PlayerGameSummaryNotification(PuzzleStateDTO personalInfo) {
        super("player_summary");
        this.personalInfo = personalInfo;
    }

    public static PlayerGameSummaryNotification create(PuzzleStateDTO personalInfo) {
        return new PlayerGameSummaryNotification(personalInfo);
    }

    public PuzzleStateDTO getPersonalInfo() {
        return personalInfo;
    }
}
