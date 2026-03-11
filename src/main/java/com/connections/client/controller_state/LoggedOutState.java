package com.connections.client.controller_state;

import com.connections.client.Controller;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * State for logged-out users.
 */
public class LoggedOutState implements ControllerState {

    private static final String CMD_REGISTER = "register";
    private static final String CMD_LOGIN = "login";

    private static final String HELP_MESSAGE = 
        "\n--- AVAILABLE COMMANDS (Logged Out) ---\n" +
        "  register <uP> <psw> : Create a new account\n" +
        "  login <uP> <psw>    : Sign in to your account\n";

    private final Controller controller;

    public LoggedOutState(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;
        boolean isLogin = false;

        switch (command) {
            case CMD_REGISTER:
                request = controller.getUserModule().prepareRegister(tokens, controller.getView());
                break;
            case CMD_LOGIN:
                request = controller.getUserModule().prepareLogin(tokens, controller.getUdpPort(), controller.getView());
                isLogin = true;
                break;
            default:
                controller.getView().showMessage("Please login or register first.");
                return;
        }

        if (request != null) {
            try {
                ServerResponse<?> response = controller.getTcpPort().sendRequest(request);
                controller.getUserModule().handleResponse(response, controller.getView());

                if (isLogin && response.getStatus() == Status.OK) {
                    controller.changeState(new LoggedInState(controller, tokens[1]));
                }
            } catch (Exception e) {
                controller.getView().showError(e.getMessage());
            }
        }
    }

    @Override
    public void printHelp() {
        controller.getView().showMessage(HELP_MESSAGE);
    }

    @Override
    public String getPrompt() {
        return "> ";
    }

    @Override
    public void onExit() {}
}
