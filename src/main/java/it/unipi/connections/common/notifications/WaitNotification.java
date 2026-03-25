package it.unipi.connections.common.notifications;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for UDP notifications sent during the waiting period between puzzles.
 * Provides the remaining seconds until the next puzzle starts.
 */
public class WaitNotification extends Notification {

    /** The unique identifier for this notification type. */
    public static final String TYPE_ID = "wait_countdown";

    @SerializedName("seconds")
    private final int secondsRemaining;

    private WaitNotification(int secondsRemaining) {
        super(TYPE_ID);
        this.secondsRemaining = secondsRemaining;
    }

    /**
     * Factory method to create a wait countdown notification.
     * 
     * @param secondsRemaining the number of seconds until the next puzzle
     * @return a new WaitNotification
     */
    public static WaitNotification create(int secondsRemaining) {
        return new WaitNotification(secondsRemaining);
    }

    public int getSecondsRemaining() { return secondsRemaining; }
}
