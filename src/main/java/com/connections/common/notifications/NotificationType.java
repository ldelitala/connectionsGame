package com.connections.common.notifications;

import com.google.gson.annotations.SerializedName;

/**
 * Enum mapping notification type IDs to their corresponding Java classes.
 * Centralizes the asynchronous protocol definition.
 */
public enum NotificationType {

    @SerializedName(NewPuzzleNotification.TYPE_ID)
    NEW_PUZZLE(NewPuzzleNotification.class),

    @SerializedName(PuzzleEndNotification.TYPE_ID)
    PUZZLE_END(PuzzleEndNotification.class),

    @SerializedName(PlayerPuzzleSummaryNotification.TYPE_ID)
    PLAYER_SUMMARY(PlayerPuzzleSummaryNotification.class),

    @SerializedName(WaitNotification.TYPE_ID)
    WAIT(WaitNotification.class);

    private final Class<? extends Notification> clazz;

    NotificationType(Class<? extends Notification> clazz) {
        this.clazz = clazz;
    }

    /** @return the Java class corresponding to this notification type. */
    public Class<? extends Notification> getNotificationClass() {
        return clazz;
    }

    /**
     * Resolves a notification type from its string identifier.
     * 
     * @param typeId the string identifier (e.g., "new_game")
     * @return the corresponding NotificationType or null if not found
     */
    public static NotificationType fromId(String typeId) {
        for (NotificationType nt : values()) {
            if (nt.name().toLowerCase().replace("_", "").equals(typeId.toLowerCase().replace("_", ""))) {
                return nt;
            }

            try {
                String constantId = (String) nt.getNotificationClass().getField("TYPE_ID").get(null);
                if (constantId.equals(typeId))
                    return nt;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
