package com.connections.common.notifications;

import com.connections.common.dto.GameInfoDTO;

/**
 * Notification sent privately to a specific player when their game ends.
 * Contains their personal results.
 */
public class PlayerGameSummaryNotification implements Notification {
    private final String type = "player_summary";
    private final GameInfoDTO personalInfo;

    private PlayerGameSummaryNotification(GameInfoDTO personalInfo) {
        this.personalInfo = personalInfo;
    }

    public static PlayerGameSummaryNotification create(GameInfoDTO personalInfo) {
        return new PlayerGameSummaryNotification(personalInfo);
    }

    @Override
    public String getType() {
        return type;
    }

    public GameInfoDTO getPersonalInfo() {
        return personalInfo;
    }
}
