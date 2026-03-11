package com.connections.client.controller.state;

import com.connections.client.controller.GameEngine;

/**
 * Interface representing a distinct operational state of the Game Engine (e.g., Logged In, Logged Out).
 * Encapsulates the state-specific logic for command handling, user feedback, and lifecycle management,
 * following the State design pattern.
 */
public interface EngineState {
    /**
     * Processes a user command according to the rules and permissions of the current state.
     * 
     * @param tokens The command and its arguments split into a string array.
     */
    void handleCommand(String[] tokens);

    /**
     * Displays a help menu containing only the commands available in this specific state.
     */
    void printHelp();

    /**
     * Retrieves the visual command prompt string associated with this state.
     * 
     * @return The state-specific prompt (e.g., "> " or "[username] > ").
     */
    String getPrompt();

    /**
     * Performs necessary cleanup or final server notifications before the application terminates.
     * This is invoked when the user issues an 'exit' command.
     */
    void onExit();
}
