package com.connections.client.controller.state;

import com.connections.client.controller.GameEngine;

/**
 * Interface for all client states (LoggedOut, LoggedIn).
 * Encapsulates behavior that changes based on session status.
 */
public interface EngineState {
    /**
     * Handles a user command.
     * 
     * @param tokens the command split into tokens.
     */
    void handleCommand(String[] tokens);

    /**
     * Prints help documentation specific to the state.
     */
    void printHelp();

    /**
     * Returns the command prompt string for the state.
     * 
     * @return the prompt string.
     */
    String getPrompt();

    /**
     * Cleanup operations when the application exits while in this state.
     */
    void onExit();
}
