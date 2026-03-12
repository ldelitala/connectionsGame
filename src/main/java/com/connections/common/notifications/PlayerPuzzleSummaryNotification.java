package com.connections.common.notifications;

import com.connections.common.dto.PuzzleStateDTO;

/**
 * Notification sent privately to a specific player when their puzzle ends.
 */
public class PlayerPuzzleSummaryNotification extends Notification {

    /** The unique identifier for this notification type. */
    public static final String TYPE_ID = "player_summary";

    private final PuzzleStateDTO personalInfo;

    private PlayerPuzzleSummaryNotification(PuzzleStateDTO personalInfo) {
        super(TYPE_ID);
        this.personalInfo = personalInfo;
    }

    public static PlayerPuzzleSummaryNotification create(PuzzleStateDTO personalInfo) {
        return new PlayerPuzzleSummaryNotification(personalInfo);
    }

    public PuzzleStateDTO getPersonalInfo() {
        return personalInfo;
    }
}
