package com.connections.client.controller.state;

import com.connections.client.controller.GameEngine;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;
import java.util.List;

/**
 * State for players who are authenticated and can participate in puzzles.
 */
public class LoggedInState implements EngineState {

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
    public void onLogout() {
        ClientRequest request = engine.getIdentityService().prepareLogout();
        if (executeRequest(request, engine.getIdentityService()) && lastStatusOk(request)) {
            engine.changeState(new LoggedOutState(engine));
        }
    }

    @Override
    public void onUpdateCredentials(String oldPass, String newName, String newPass) {
        String[] tokens = new String[]{"update", oldPass, newName, newPass};
        ClientRequest request = engine.getIdentityService().prepareUpdate(username, tokens);
        if (executeRequest(request, engine.getIdentityService()) && lastStatusOk(request)) {
            UserRequest userReq = (UserRequest) request;
            if (userReq.getNewName() != null) this.username = userReq.getNewName();
        }
    }

    @Override
    public void onGuess(List<String> words) {
        // Mocking tokens for legacy preparation method
        String[] tokens = new String[words.size() + 1];
        tokens[0] = "guess";
        for(int i=0; i<words.size(); i++) tokens[i+1] = words.get(i);
        
        ClientRequest request = engine.getGameplayService().prepareSubmit(tokens);
        executeRequest(request, engine.getGameplayService());
    }

    @Override public void onRequestBoard() { executeRequest(engine.getGameplayService().prepareStatus(), engine.getGameplayService()); }
    @Override public void onRequestStats() { executeRequest(engine.getGameplayService().prepareStats(), engine.getGameplayService()); }
    
    @Override
    public void onRequestPastInfo(int puzzleId) {
        ClientRequest request = engine.getArchiveService().preparePastInfo(new String[]{"info", String.valueOf(puzzleId)});
        executeRequest(request, engine.getArchiveService());
    }

    @Override
    public void onRequestGlobalStats(int puzzleId) {
        ClientRequest request = engine.getArchiveService().prepareGlobalStats(new String[]{"global", String.valueOf(puzzleId)});
        executeRequest(request, engine.getArchiveService());
    }

    @Override
    public void onRequestRanking(String username, Integer topK) {
        String[] tokens = (topK != null) ? new String[]{"ranking", "-top", String.valueOf(topK)} : new String[]{"ranking", username};
        ClientRequest request = engine.getArchiveService().prepareRanking(tokens);
        executeRequest(request, engine.getArchiveService());
    }

    @Override public void onRequestHistory() { executeRequest(engine.getArchiveService().prepareHistory(), engine.getArchiveService()); }

    // Helpers
    private boolean executeRequest(ClientRequest request, Object service) {
        if (request == null) return false;
        try {
            ServerResponse<?> response = engine.getGateway().sendRequest(request);
            if (service instanceof com.connections.client.services.IdentityService) ((com.connections.client.services.IdentityService) service).handleResponse(response);
            else if (service instanceof com.connections.client.services.GameplayService) ((com.connections.client.services.GameplayService) service).handleResponse(response);
            else if (service instanceof com.connections.client.services.ArchiveService) ((com.connections.client.services.ArchiveService) service).handleResponse(response);
            return response.getStatus() == Status.OK;
        } catch (Exception e) {
            engine.getView().showError(e.getMessage());
            return false;
        }
    }

    private boolean lastStatusOk(ClientRequest request) { return true; /* Already checked in executeRequest */ }

    @Override public void onRegister(String u, String p) { engine.getView().showMessage("Already logged in."); }
    @Override public void onLogin(String u, String p) { engine.getView().showMessage("Already logged in."); }
    @Override public void printHelp() { engine.getView().showMessage(HELP_MESSAGE); }
    @Override public String getPrompt() { return String.format("[%s] > ", username); }
    @Override public void onExit() { try { engine.getGateway().sendRequest(engine.getIdentityService().prepareLogout()); } catch (Exception ignored) {} }
}
