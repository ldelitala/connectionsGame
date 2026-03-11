package com.connections.common.notifications;

import com.connections.common.dto.PuzzleStateDTO;

/**
 * Notification sent privately to a specific player when their puzzle ends.
 * Contains their personal results.
 */
public class PlayerGameSummaryNotification implements Notification {
    private final String type = "player_summary";
    private final PuzzleStateDTO personalInfo;

    private PlayerGameSummaryNotification(PuzzleStateDTO personalInfo) {
        this.personalInfo = personalInfo;
    }

    public static PlayerGameSummaryNotification create(PuzzleStateDTO personalInfo) {
        return new PlayerGameSummaryNotification(personalInfo);
    }

    @Override
    public String getType() {
        return type;
    }

    public PuzzleStateDTO getPersonalInfo() {
        return personalInfo;
    }
}
