package com.connections.client.engine.state;

import com.connections.client.engine.GameEngine;
import com.connections.common.protocol.server_responses.Status;
import java.util.List;

/**
 * State for players who are authenticated. Actions are delegated to the appropriate Module which
 * maintains its own gateway.
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
        Status status = engine.getUserModule().handleLogout();
        if (status == Status.OK) {
            engine.changeState(new LoggedOutState(engine));
        }
    }

    @Override
    public void onUpdateCredentials(String oldPass, String newName, String newPass) {
        Status status = engine.getUserModule().handleUpdate(username, oldPass, newName, newPass);
        if (status == Status.OK && newName != null) {
            this.username = newName;
        }
    }

    @Override
    public void onGuess(List<String> words) {
        engine.getGameplayModule().handleGuess(words);
    }

    @Override
    public void onRequestBoard() {
        engine.getGameplayModule().handleStatus();
    }

    @Override
    public void onRequestStats() {
        engine.getGameplayModule().handleStats();
    }

    @Override
    public void onRequestPastInfo(int puzzleId) {
        engine.getHistoryModule().handlePastInfo(puzzleId);
    }

    @Override
    public void onRequestGlobalStats(int puzzleId) {
        engine.getHistoryModule().handleGlobalStats(puzzleId);
    }

    @Override
    public void onRequestRanking(String username, Integer topK) {
        engine.getHistoryModule().handleRanking(username, topK);
    }

    @Override
    public void onRequestHistory() {
        engine.getHistoryModule().handleHistory();
    }

    @Override
    public String getPrompt() {
        return String.format("[%s] > ", username);
    }

    @Override
    public void onExit() {
        engine.getUserModule().handleLogout();
    }

    @Override
    public void onHelp() {
        engine.getView().showHelp(true);
    }
}
