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
    private static final String MSG_NOT_LOGGED = "Command not available. Please login to play.";
    private static final String ERR_PREFIX = "Communication error: ";
    private static final String PROMPT = "> ";

    // --- Help Messages ---
    private static final String HELP_HEADER = "\n--- AVAILABLE COMMANDS (Logged Out) ---";
    private static final String HELP_REGISTER = "  register [u] [p]  : Register a new account";
    private static final String HELP_LOGIN = "  login [u] [p]     : Sign in to the game";
    private static final String HELP_HELP = "  help              : Show this list";
    private static final String HELP_EXIT = "  exit              : Close the application\n";

    // --- Indices ---
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
                System.out.println(MSG_NOT_LOGGED);
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
                System.err.println(ERR_PREFIX + e.getMessage());
            }
        }
    }


    @Override
    public void printHelp() {
        System.out.println(HELP_HEADER);
        System.out.println(HELP_REGISTER);
        System.out.println(HELP_LOGIN);
        System.out.println(HELP_HELP);
        System.out.println(HELP_EXIT);
    }

    @Override
    public String getPrompt() {
        return PROMPT;
    }
}
