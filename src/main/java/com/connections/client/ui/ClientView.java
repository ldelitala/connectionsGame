package com.connections.client.ui;

import com.connections.common.protocol.server_responses.dto.DTO;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.notifications.Notification;

/**
 * Interface defining all possible output operations for the client. Uses a dispatcher-based
 * approach for rendering data and notifications.
 */
public interface ClientView {
    /** General user feedback for server responses. */
    void showResponse(ServerResponse<?> response);

    /**
     * Dispatcher for DTO rendering based on type. See dto package for specific DTO types..
     */
    void render(DTO data);

    /**
     * Dispatcher for asynchronous notifications rendering. See notifications package (or
     * notificationType enum)for specific notification types.
     */
    void render(Notification notification);

    /** Displays the help menu based on the current authentication state. */
    void showHelp(boolean loggedIn);

    /** Generic error display. */
    void showError(String error);

    /** Generic message display. */
    void showMessage(String message);

    /** Renders the command line prompt. */
    void renderPrompt(String prompt);
}
