package com.connections.client.controller_state;

import java.io.IOException;
import com.connections.client.Controller;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * Represents the client state when the user is not authenticated (Logged Out).
 */
public class LoggedOutState implements ControllerState {

    // --- Commands ---
    private static final String CMD_REGISTER = "register";
    private static final String CMD_LOGIN = "login";

    // --- Messages ---
    private static final String HELP_MESSAGE = 
        "\n--- AVAILABLE COMMANDS (Logged Out) ---\n" +
        "  register [u] [p]  : Register a new account\n" +
        "  login [u] [p]     : Sign in to the game\n";

    // --- Configuration ---
    private static final String PROMPT = "> ";
    private static final int CMD_INDEX = 0;
    private static final int USERNAME_INDEX = 1;
    private static final int MIN_TOKENS_FOR_USERNAME = 2;

    private final Controller controller;

    public LoggedOutState(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[CMD_INDEX].toLowerCase();
        ClientRequest request = null;
        boolean tryLogin = false;

        switch (command) {
            case CMD_REGISTER:
                request = controller.getUserModule().prepareRegister(tokens);
                break;

            case CMD_LOGIN:
                request = controller.getUserModule().prepareLogin(tokens);
                tryLogin = true;
                break;

            default:
                System.out.println("Command not available. Please login to play.");
                return;
        }

        if (request != null) {
            try {
                ServerResponse<?> response = controller.getTcpPort().sendRequest(request);

                controller.getUserModule().displayResponse(response);

                if (tryLogin && response.getStatus() == Status.OK) {
                    // Get username from request if not in tokens
                    String username = (tokens.length >= MIN_TOKENS_FOR_USERNAME) ? tokens[USERNAME_INDEX] : ((com.connections.common.protocol.client_requests.UserRequest)request).getUsername();
                    controller.changeState(new LoggedInState(controller, username));
                }

            } catch (IOException e) {
                System.err.println("Communication error: " + e.getMessage());
            }
        }
    }

    @Override
    public void printHelp() {
        System.out.println(HELP_MESSAGE);
    }

    @Override
    public String getPrompt() {
        return PROMPT;
    }
}
