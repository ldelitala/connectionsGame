package com.connections.common.notifications;

/**
 * Base abstract class for all asynchronous UDP notifications.
 * Ensures that every notification includes a mandatory 'type' field for protocol identification.
 */
public abstract class Notification {

    /** The JSON field name used for identification. */
    public static final String FIELD_TYPE = "type";
    
    /** The identifier for the notification type. */
    public final String type;

    /**
     * Constructs a new notification with the specified type.
     * 
     * @param type the unique string identifier for this notification type.
     */
    protected Notification(String type) {
        this.type = type;
    }

    /**
     * Returns the type identifier of the notification.
     * 
     * @return the notification type string.
     */
    public String getType() {
        return type;
    }
}
