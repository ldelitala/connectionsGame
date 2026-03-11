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

    private final GameEngine engine;
    private String username;

    public LoggedInState(GameEngine engine, String username) {
        this.engine = engine;
        this.username = username;
    }

    @Override public void onRegister(String u, String p) { engine.getView().showMessage("Already logged in."); }
    @Override public void onLogin(String u, String p) { engine.getView().showMessage("Already logged in."); }

    @Override
    public void onLogout() {
        ClientRequest request = engine.getIdentityAdapter().prepareLogout();
        if (executeRequest(request, engine.getIdentityAdapter())) {
            engine.changeState(new LoggedOutState(engine));
        }
    }

    @Override
    public void onUpdateCredentials(String oldPass, String newName, String newPass) {
        ClientRequest request = engine.getIdentityAdapter().prepareUpdate(username, oldPass, newName, newPass);
        if (executeRequest(request, engine.getIdentityAdapter())) {
            if (newName != null && !"-".equals(newName)) this.username = newName;
        }
    }

    @Override
    public void onGuess(List<String> words) {
        executeRequest(engine.getGameplayAdapter().prepareGuess(words), engine.getGameplayAdapter());
    }

    @Override public void onRequestBoard() { executeRequest(engine.getGameplayAdapter().prepareStatus(), engine.getGameplayAdapter()); }
    @Override public void onRequestStats() { executeRequest(engine.getGameplayAdapter().prepareStats(), engine.getGameplayAdapter()); }
    
    @Override
    public void onRequestPastInfo(int puzzleId) {
        executeRequest(engine.getHistoryAdapter().preparePastInfo(puzzleId), engine.getHistoryAdapter());
    }

    @Override
    public void onRequestGlobalStats(int puzzleId) {
        executeRequest(engine.getHistoryAdapter().prepareGlobalStats(puzzleId), engine.getHistoryAdapter());
    }

    @Override
    public void onRequestRanking(String username, Integer topK) {
        executeRequest(engine.getHistoryAdapter().prepareRanking(username, topK), engine.getHistoryAdapter());
    }

    @Override public void onRequestHistory() { executeRequest(engine.getHistoryAdapter().prepareHistory(), engine.getHistoryAdapter()); }

    private boolean executeRequest(ClientRequest request, Object adapter) {
        if (request == null) return false;
        try {
            ServerResponse<?> response = engine.getGateway().sendRequest(request);
            if (adapter instanceof com.connections.client.adapters.IdentityAdapter) ((com.connections.client.adapters.IdentityAdapter) adapter).handleResponse(response);
            else if (adapter instanceof com.connections.client.adapters.GameplayAdapter) ((com.connections.client.adapters.GameplayAdapter) adapter).handleResponse(response);
            else if (adapter instanceof com.connections.client.adapters.HistoryAdapter) ((com.connections.client.adapters.HistoryAdapter) adapter).handleResponse(response);
            return response.getStatus() == Status.OK;
        } catch (Exception e) {
            engine.getView().showError(e.getMessage());
            return false;
        }
    }

    @Override
    public void printHelp() {
        engine.getView().showMessage("\n--- AVAILABLE COMMANDS (Logged In) ---\n" +
                                     "Active Puzzle: guess, board, stats\n" +
                                     "History: info, global, ranking, history\n" +
                                     "Account: update, logout\n");
    }

    @Override public String getPrompt() { return String.format("[%s] > ", username); }
    @Override public void onExit() { try { engine.getGateway().sendRequest(engine.getIdentityAdapter().prepareLogout()); } catch (Exception ignored) {} }
}
