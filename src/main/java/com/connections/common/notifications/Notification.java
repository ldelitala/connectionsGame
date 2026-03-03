package com.connections.common.notifications;

/**
 * Common interface for all asynchronous UDP notifications.
 */
public interface Notification {
    /**
     * Returns the type identifier of the notification.
     * 
     * @return a string identifying the notification type
     */
    String getType();
}
