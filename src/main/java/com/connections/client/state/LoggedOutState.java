package com.connections.client.state;

import java.io.IOException;
import com.connections.client.ClientController;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * Represents the client state when the user is not authenticated (Logged Out).
 * In this state, the only allowed operations are registration and login.
 * Implements the State pattern to manage contextual client behavior.
 */
public class LoggedOutState implements ClientState {
    
    /** The main controller acting as the context for the state. */
    private final ClientController controller;
    
    /** Internal flag to track if the last requested operation was a login attempt. */
    private boolean tryLogin = false;

    /**
     * Constructs a new LoggedOutState.
     * 
     * @param controller the controller managing the client's state machine.
     */
    public LoggedOutState(ClientController controller) {
        this.controller = controller;
    }

    /**
     * Handles user commands by filtering them for the disconnected state.
     * Supports 'register' and 'login' commands. If a command is not supported in this
     * state, it informs the user of the need to authenticate.
     * 
     * @param tokens the array of strings composing the command entered by the user.
     */
    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;

        switch (command) {
            case "register":
                request = controller.getUserModule().prepareRegister(tokens);
                break;

            case "login":
                request = controller.getUserModule().prepareLogin(tokens);
                tryLogin = true;
                break;

            default:
                System.out.println("Command not available. Please login to play.");
                return;
        }

        if (request != null) {
            try {
                // Send request to server via the TCP port
                ServerResponse<?> response = controller.getTcpPort().sendRequest(request);
                
                // Display operation result via the user module
                controller.getUserModule().displayResponse(response);

                // If login was successful, transition to LoggedInState
                if (tryLogin && response.getStatus() == Status.OK) {
                    // tokens[1] contains the username used for login
                    controller.changeState(new LoggedInState(controller, tokens[1]));
                    tryLogin = false; 
                }

            } catch (IOException e) {
                System.err.println("Communication error: " + e.getMessage());
            }
        }
    }

    /**
     * Prints contextual help for the logged-out state.
     * Shows only registration, login, and exit commands.
     */
    @Override
    public void printHelp() {
        System.out.println("\n--- AVAILABLE COMMANDS (Logged Out) ---");
        System.out.println("  register [u] [p]  : Register a new account");
        System.out.println("  login [u] [p]     : Sign in to the game");
        System.out.println("  help              : Show this list");
        System.out.println("  exit              : Close the application\n");
    }

    /**
     * Returns the basic command prompt for an unauthenticated user.
     * 
     * @return the prompt string "> ".
     */
    @Override
    public String getPrompt() {
        return "> ";
    }
}
