package com.connections.client.state;

/**
 * Interface for all client states (LoggedOut, LoggedIn).
 * Encapsulates behavior that changes based on session status.
 */
public interface ControllerState {
    /**
     * Handles a user command.
     * @param tokens the command split into tokens
     */
    void handleCommand(String[] tokens);

    /**
     * Prints help specific to this state.
     */
    void printHelp();

    /**
     * Returns the dynamic prompt for this state (e.g., "> " or "[user] > ").
     */
    String getPrompt();
}
