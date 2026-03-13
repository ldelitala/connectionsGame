package com.connections.client.engine.state;

import com.connections.client.engine.GameEngine;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.Status;
import java.util.List;

/**
 * State for players who are authenticated.
 */
public class LoggedInState implements EngineState {

    private final GameEngine engine;
    private String username;

    public LoggedInState(GameEngine engine, String username) {
        this.engine = engine;
        this.username = username;
    }

    @Override
    public void onRegister(String u, String p) {
        engine.getView().showMessage("Already logged in.");
    }

    @Override
    public void onLogin(String u, String p) {
        engine.getView().showMessage("Already logged in.");
    }

    @Override
    public void onLogout() {
        Status status = engine.getExecutor().execute(UserRequest.logout());
        if (status == Status.OK) {
            engine.changeState(new LoggedOutState(engine));
        }
    }

    @Override
    public void onUpdateCredentials(String oldPass, String newName, String newPass) {
        if (newName == null && newPass == null) {
            engine.getView().showMessage("No changes provided.");
            return;
        }
        Status status = engine.getExecutor().execute(UserRequest.updateCredentials(username, oldPass, newName, newPass));
        if (status == Status.OK && newName != null && !newName.equals("-")) {
            this.username = newName;
        }
    }

    @Override
    public void onGuess(List<String> words) {
        engine.getExecutor().execute(GameplayRequest.submitProposal(words));
    }

    @Override
    public void onRequestBoard() {
        engine.getExecutor().execute(GameplayRequest.getCurrentInfo());
    }

    @Override
    public void onRequestStats() {
        engine.getExecutor().execute(GameplayRequest.getCurrentStats());
    }

    @Override
    public void onRequestPastInfo(int puzzleId) {
        engine.getExecutor().execute(HistoryRequest.getPuzzleInfo(puzzleId));
    }

    @Override
    public void onRequestGlobalStats(int puzzleId) {
        engine.getExecutor().execute(HistoryRequest.getPuzzleStats(puzzleId));
    }

    @Override
    public void onRequestRanking(String username, Integer topK) {
        engine.getExecutor().execute(HistoryRequest.getLeaderboard(username, topK));
    }

    @Override
    public void onRequestHistory() {
        engine.getExecutor().execute(HistoryRequest.getPlayerStats());
    }

    @Override
    public String getPrompt() {
        return String.format("[%s] > ", username);
    }

    @Override
    public void onExit() {
        engine.getExecutor().execute(UserRequest.logout());
    }

    @Override
    public void onHelp() {
        engine.getView().showHelp(true);
    }
}
