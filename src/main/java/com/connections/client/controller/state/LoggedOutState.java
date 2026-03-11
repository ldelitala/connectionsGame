package com.connections.client.controller.state;

import com.connections.client.controller.GameEngine;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * State for logged-out users.
 */
public class LoggedOutState implements EngineState {

    private static final String CMD_REGISTER = "register";
    private static final String CMD_LOGIN = "login";

    private static final String HELP_MESSAGE = 
        "\n--- AVAILABLE COMMANDS (Logged Out) ---\n" +
        "  register <uP> <psw> : Create a new account\n" +
        "  login <uP> <psw>    : Sign in to your account\n";

    private final GameEngine engine;

    /**
     * Initializes a new state for a guest user.
     *
     * @param engine The parent GameEngine.
     */
    public LoggedOutState(GameEngine engine) {
        this.engine = engine;
    }

    /**
     * Handles commands for registration and login.
     * Switches state to {@link LoggedInState} upon successful authentication.
     *
     * @param tokens The command split into tokens.
     */
    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;
        boolean isLogin = false;

        switch (command) {
            case CMD_REGISTER:
                request = engine.getIdentityService().prepareRegister(tokens);
                break;
            case CMD_LOGIN:
                request = engine.getIdentityService().prepareLogin(tokens, engine.getUdpPort());
                isLogin = true;
                break;
            default:
                engine.getView().showMessage("Please login or register first.");
                return;
        }

        if (request != null) {
            try {
                ServerResponse<?> response = engine.getGateway().sendRequest(request);
                engine.getIdentityService().handleResponse(response);

                if (isLogin && response.getStatus() == Status.OK) {
                    engine.changeState(new LoggedInState(engine, tokens[1]));
                }
            } catch (Exception e) {
                engine.getView().showError(e.getMessage());
            }
        }
    }

    /**
     * Displays the guest user's command menu.
     */
    @Override
    public void printHelp() {
        engine.getView().showMessage(HELP_MESSAGE);
    }

    /**
     * Returns the generic guest prompt.
     *
     * @return The guest prompt.
     */
    @Override
    public String getPrompt() {
        return "> ";
    }

    /**
     * No cleanup required for guest users.
     */
    @Override
    public void onExit() {}
}
