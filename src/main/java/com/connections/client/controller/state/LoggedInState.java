package com.connections.client.controller.state;

import com.connections.client.controller.Controller;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.UserRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * State for logged-in users.
 */
public class LoggedInState implements ControllerState {

    private static final String CMD_LOGOUT = "logout";
    private static final String CMD_UPDATE = "update";
    private static final String CMD_GUESS = "guess";
    private static final String CMD_BOARD = "board";
    private static final String CMD_STATS = "stats";
    private static final String CMD_INFO = "info";
    private static final String CMD_GLOBAL = "global";
    private static final String CMD_RANKING = "ranking";
    private static final String CMD_HISTORY = "history";

    private static final String HELP_MESSAGE = 
        "\n--- AVAILABLE COMMANDS (Logged In) ---\n" +
        "Active Puzzle:\n" +
        "  guess [w1-w4]     : Send a 4-word attempt\n" +
        "  board             : View current puzzle board (mistakes, found categories)\n" +
        "  stats             : View global participation stats for this puzzle\n" +
        "\nHistory and Rankings:\n" +
        "  info [id]         : View details and solution of a past puzzle\n" +
        "  global [id]       : View global statistics of a past puzzle\n" +
        "  ranking <u|-top k>: View rank of user <u> or top k players\n" +
        "  history           : View your personal overall statistics\n" +
        "\nAccount:\n" +
        "  update <oP> <nU> <nP> : Change your username or password\n" + 
        "  logout            : Sign out of your account\n";

    private final Controller controller;
    private String username;

    public LoggedInState(Controller controller, String username) {
        this.controller = controller;
        this.username = username;
    }

    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;
        Object module = null;

        switch (command) {
            case CMD_LOGOUT:
                request = controller.getUserModule().prepareLogout();
                module = controller.getUserModule();
                break;
            case CMD_UPDATE:
                request = controller.getUserModule().prepareUpdate(username, tokens, controller.getView());
                module = controller.getUserModule();
                break;
            case CMD_GUESS:
                request = controller.getGameModule().prepareSubmit(tokens, controller.getView());
                module = controller.getGameModule();
                break;
            case CMD_BOARD:
                request = controller.getGameModule().prepareStatus();
                module = controller.getGameModule();
                break;
            case CMD_STATS:
                request = controller.getGameModule().prepareStats();
                module = controller.getGameModule();
                break;
            case CMD_INFO:
                request = controller.getHistoryModule().preparePastInfo(tokens, controller.getView());
                module = controller.getHistoryModule();
                break;
            case CMD_GLOBAL:
                request = controller.getHistoryModule().prepareGlobalStats(tokens, controller.getView());
                module = controller.getHistoryModule();
                break;
            case CMD_RANKING:
                request = controller.getHistoryModule().prepareRanking(tokens, controller.getView());
                module = controller.getHistoryModule();
                break;
            case CMD_HISTORY:
                request = controller.getHistoryModule().prepareHistory();
                module = controller.getHistoryModule();
                break;
            default:
                controller.getView().showMessage("Unknown command. Type 'help' for assistance.");
                return;
        }

        if (request != null) {
            try {
                ServerResponse<?> response = controller.getTcpPort().sendRequest(request);
                
                if (module instanceof com.connections.client.modules.UserModule) {
                    ((com.connections.client.modules.UserModule) module).handleResponse(response, controller.getView());
                    if (CMD_LOGOUT.equals(command) && response.getStatus() == Status.OK) {
                        controller.changeState(new LoggedOutState(controller));
                    } else if (CMD_UPDATE.equals(command) && response.getStatus() == Status.OK) {
                        UserRequest userReq = (UserRequest) request;
                        if (userReq.getNewName() != null) this.username = userReq.getNewName();
                    }
                } else if (module instanceof com.connections.client.modules.GameModule) {
                    ((com.connections.client.modules.GameModule) module).handleResponse(response, controller.getView());
                } else if (module instanceof com.connections.client.modules.HistoryModule) {
                    ((com.connections.client.modules.HistoryModule) module).handleResponse(response, controller.getView());
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
        return String.format("[%s] > ", username);
    }

    @Override
    public void onExit() {
        try {
            controller.getTcpPort().sendRequest(controller.getUserModule().prepareLogout());
        } catch (Exception ignored) {}
    }
}
