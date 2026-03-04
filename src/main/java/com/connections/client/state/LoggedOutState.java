package com.connections.client.state;

import java.io.IOException;
import com.connections.client.Controller;
import com.connections.client.modules.ModuleView;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * Represents the client state when the user is not authenticated (Logged Out).
 */
public class LoggedOutState implements ControllerState {

    private final Controller controller;

    public LoggedOutState(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;
        boolean tryLogin = false;

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
                ServerResponse<?> response = controller.getTcpPort().sendRequest(request);

                controller.getUserModule().displayResponse(response);

                if (tryLogin && response.getStatus() == Status.OK) {
                    // Get username from request if not in tokens
                    String username = (tokens.length >= 2) ? tokens[1] : ((com.connections.common.protocol.client_requests.UserRequest)request).getUsername();
                    controller.changeState(new LoggedInState(controller, username));
                }

            } catch (IOException e) {
                System.err.println("Communication error: " + e.getMessage());
            }
        }
    }


    @Override
    public void printHelp() {
        System.out.println("\n--- AVAILABLE COMMANDS (Logged Out) ---");
        System.out.println("  register [u] [p]  : Register a new account");
        System.out.println("  login [u] [p]     : Sign in to the game");
        System.out.println("  help              : Show this list");
        System.out.println("  exit              : Close the application\n");
    }

    @Override
    public String getPrompt() {
        return "> ";
    }
}
