package com.connections.client.state;

import java.io.IOException;
import com.connections.client.ClientController;
import com.connections.client.modules.ModuleView;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.Operation;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * Represents the client state when the user is not authenticated (Logged Out).
 */
public class LoggedOutState implements ClientState {
    
    private final ClientController controller;
    private boolean tryLogin = false;

    public LoggedOutState(ClientController controller) {
        this.controller = controller;
    }

    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;
        ModuleView activeModule = controller.getUserModule();

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
            executeAndDisplay(request, activeModule, tokens);
        }
    }

    private void executeAndDisplay(ClientRequest request, ModuleView activeModule, String[] tokens) {
        try {
            ServerResponse<?> response = controller.getTcpPort().sendRequest(request);
            
            // Polymorphic call: no ifs, no casts
            activeModule.displayResponse(response);

            if (tryLogin && response.getStatus() == Status.OK) {
                // tokens[1] contains the username used for login
                controller.changeState(new LoggedInState(controller, tokens[1]));
                tryLogin = false; 
            }

        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
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
