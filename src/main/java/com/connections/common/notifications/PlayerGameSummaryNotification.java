package com.connections.common.notifications;

import com.connections.common.dto.PuzzleStateDTO;

/**
 * Notification sent privately to a specific player when their puzzle ends.
 */
public class PlayerGameSummaryNotification extends Notification {

    /** The unique identifier for this notification type. */
    public static final String TYPE_ID = "player_summary";

    private final PuzzleStateDTO personalInfo;

    private PlayerGameSummaryNotification(PuzzleStateDTO personalInfo) {
        super(TYPE_ID);
        this.personalInfo = personalInfo;
    }

    public static PlayerGameSummaryNotification create(PuzzleStateDTO personalInfo) {
        return new PlayerGameSummaryNotification(personalInfo);
    }

    public PuzzleStateDTO getPersonalInfo() {
        return personalInfo;
    }
}
