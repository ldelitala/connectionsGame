package com.connections.client.controller.state;

import com.connections.client.controller.GameEngine;
import com.connections.common.protocol.server_responses.Status;
import java.util.List;

/**
 * State for players who are not authenticated.
 * Actions are delegated to the UserAdapter which now maintains its own gateway.
 */
public class LoggedOutState implements EngineState {

    private final GameEngine engine;

    public LoggedOutState(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onRegister(String username, String password) {
        engine.getUserAdapter().handleRegister(username, password);
    }

    @Override
    public void onLogin(String username, String password) {
        Status status = engine.getUserAdapter().handleLogin(username, password, engine.getUdpPort());
        if (status == Status.OK) {
            engine.changeState(new LoggedInState(engine, username));
        }
    }

    @Override public void onLogout() { engine.getView().showMessage("Please login first."); }
    @Override public void onUpdateCredentials(String oP, String nU, String nP) { engine.getView().showMessage("Please login first."); }
    @Override public void onGuess(List<String> words) { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestBoard() { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestStats() { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestPastInfo(int id) { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestGlobalStats(int id) { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestRanking(String u, Integer k) { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestHistory() { engine.getView().showMessage("Please login first."); }

    @Override
    public void printHelp() {
        engine.getView().showMessage("\n--- AVAILABLE COMMANDS (Logged Out) ---\n" +
                                     "  register <uP> <psw>\n  login <uP> <psw>\n");
    }

    @Override public String getPrompt() { return "> "; }
    @Override public void onExit() {}
}
