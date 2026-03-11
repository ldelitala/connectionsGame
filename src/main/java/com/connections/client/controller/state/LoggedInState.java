package com.connections.client.controller.state;

import com.connections.client.controller.GameEngine;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.UserRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * State for logged-in players.
 */
public class LoggedInState implements EngineState {

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
        "  board             : View current puzzle board\n" +
        "  stats             : View global stats for this puzzle\n" +
        "\nHistory: info, global, ranking, history\n" +
        "Account: update, logout\n";

    private final GameEngine engine;
    private String username;

    public LoggedInState(GameEngine engine, String username) {
        this.engine = engine;
        this.username = username;
    }

    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;
        Object service = null;

        switch (command) {
            case CMD_LOGOUT:
                request = engine.getIdentityService().prepareLogout();
                service = engine.getIdentityService();
                break;
            case CMD_UPDATE:
                request = engine.getIdentityService().prepareUpdate(username, tokens, engine.getView());
                service = engine.getIdentityService();
                break;
            case CMD_GUESS:
                request = engine.getGameplayService().prepareSubmit(tokens, engine.getView());
                service = engine.getGameplayService();
                break;
            case CMD_BOARD:
                request = engine.getGameplayService().prepareStatus();
                service = engine.getGameplayService();
                break;
            case CMD_STATS:
                request = engine.getGameplayService().prepareStats();
                service = engine.getGameplayService();
                break;
            case CMD_INFO:
                request = engine.getArchiveService().preparePastInfo(tokens, engine.getView());
                service = engine.getArchiveService();
                break;
            case CMD_GLOBAL:
                request = engine.getArchiveService().prepareGlobalStats(tokens, engine.getView());
                service = engine.getArchiveService();
                break;
            case CMD_RANKING:
                request = engine.getArchiveService().prepareRanking(tokens, engine.getView());
                service = engine.getArchiveService();
                break;
            case CMD_HISTORY:
                request = engine.getArchiveService().prepareHistory();
                service = engine.getArchiveService();
                break;
            default:
                engine.getView().showMessage("Unknown command.");
                return;
        }

        if (request != null) {
            try {
                ServerResponse<?> response = engine.getTcpPort().sendRequest(request);
                
                if (service instanceof com.connections.client.services.IdentityService) {
                    ((com.connections.client.services.IdentityService) service).handleResponse(response, engine.getView());
                    if (CMD_LOGOUT.equals(command) && response.getStatus() == Status.OK) {
                        engine.changeState(new LoggedOutState(engine));
                    } else if (CMD_UPDATE.equals(command) && response.getStatus() == Status.OK) {
                        UserRequest userReq = (UserRequest) request;
                        if (userReq.getNewName() != null) this.username = userReq.getNewName();
                    }
                } else if (service instanceof com.connections.client.services.GameplayService) {
                    ((com.connections.client.services.GameplayService) service).handleResponse(response, engine.getView());
                } else if (service instanceof com.connections.client.services.ArchiveService) {
                    ((com.connections.client.services.ArchiveService) service).handleResponse(response, engine.getView());
                }

            } catch (Exception e) {
                engine.getView().showError(e.getMessage());
            }
        }
    }

    @Override
    public void printHelp() {
        engine.getView().showMessage(HELP_MESSAGE);
    }

    @Override
    public String getPrompt() {
        return String.format("[%s] > ", username);
    }

    @Override
    public void onExit() {
        try {
            engine.getTcpPort().sendRequest(engine.getIdentityService().prepareLogout());
        } catch (Exception ignored) {}
    }
}
