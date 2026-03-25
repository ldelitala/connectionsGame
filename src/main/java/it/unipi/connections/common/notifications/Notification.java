package it.unipi.connections.common.notifications;

/**
 * Base abstract class for all asynchronous UDP notifications.
 * Ensures that every notification includes a mandatory 'type' field for
 * protocol identification.
 */
public abstract class Notification {

    /** The JSON field name used for identification. */
    public static final String FIELD_TYPE = "type";

    /** The identifier for the notification type. */
    public final String type;

    protected Notification(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
