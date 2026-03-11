package com.connections.client.controller.state;

import com.connections.client.controller.GameEngine;
import com.connections.common.protocol.server_responses.Status;
import java.util.List;

/**
 * State for players who are authenticated.
 * Actions are delegated to the appropriate Adapter with the network gateway.
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
        Status status = engine.getUserAdapter().handleLogout(engine.getGateway());
        if (status == Status.OK) {
            engine.changeState(new LoggedOutState(engine));
        }
    }

    @Override
    public void onUpdateCredentials(String oldPass, String newName, String newPass) {
        Status status = engine.getUserAdapter().handleUpdate(username, oldPass, newName, newPass, engine.getGateway());
        if (status == Status.OK && newName != null && !"-".equals(newName)) {
            this.username = newName;
        }
    }

    @Override
    public void onGuess(List<String> words) {
        engine.getGameplayAdapter().handleGuess(words, engine.getGateway());
    }

    @Override
    public void onRequestBoard() {
        engine.getGameplayAdapter().handleStatus(engine.getGateway());
    }

    @Override
    public void onRequestStats() {
        engine.getGameplayAdapter().handleStats(engine.getGateway());
    }

    @Override
    public void onRequestPastInfo(int puzzleId) {
        engine.getHistoryAdapter().handlePastInfo(puzzleId, engine.getGateway());
    }

    @Override
    public void onRequestGlobalStats(int puzzleId) {
        engine.getHistoryAdapter().handleGlobalStats(puzzleId, engine.getGateway());
    }

    @Override
    public void onRequestRanking(String username, Integer topK) {
        engine.getHistoryAdapter().handleRanking(username, topK, engine.getGateway());
    }

    @Override
    public void onRequestHistory() {
        engine.getHistoryAdapter().handleHistory(engine.getGateway());
    }

    @Override
    public void printHelp() {
        engine.getView().showMessage("\n--- AVAILABLE COMMANDS (Logged In) ---\n" +
                                     "Active Puzzle: guess, board, stats\n" +
                                     "History: info, global, ranking, history\n" +
                                     "Account: update, logout\n");
    }

    @Override public String getPrompt() { return String.format("[%s] > ", username); }
    
    @Override 
    public void onExit() { 
        engine.getUserAdapter().handleLogout(engine.getGateway());
    }
}
