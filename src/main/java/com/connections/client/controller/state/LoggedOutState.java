package com.connections.client.controller.state;

import com.connections.client.controller.GameEngine;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;
import java.util.List;

/**
 * State for players who are not authenticated.
 */
public class LoggedOutState implements EngineState {

    private static final String HELP_MESSAGE = 
        "\n--- AVAILABLE COMMANDS (Logged Out) ---\n" +
        "  register <uP> <psw> : Create a new account\n" +
        "  login <uP> <psw>    : Sign in to your account\n";

    private final GameEngine engine;

    public LoggedOutState(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onRegister(String username, String password) {
        ClientRequest request = engine.getIdentityService().prepareRegister(username, password);
        executeRequest(request, false, username);
    }

    @Override
    public void onLogin(String username, String password) {
        ClientRequest request = engine.getIdentityService().prepareLogin(username, password, engine.getUdpPort());
        executeRequest(request, true, username);
    }

    private void executeRequest(ClientRequest request, boolean isLogin, String username) {
        if (request == null) return;
        try {
            ServerResponse<?> response = engine.getGateway().sendRequest(request);
            engine.getIdentityService().handleResponse(response);
            if (isLogin && response.getStatus() == Status.OK) {
                engine.changeState(new LoggedInState(engine, username));
            }
        } catch (Exception e) {
            engine.getView().showError(e.getMessage());
        }
    }

    @Override public void onLogout() { engine.getView().showMessage("You are not logged in."); }
    @Override public void onUpdateCredentials(String oP, String nU, String nP) { engine.getView().showMessage("Please login first."); }
    @Override public void onGuess(List<String> words) { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestBoard() { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestStats() { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestPastInfo(int id) { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestGlobalStats(int id) { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestRanking(String u, Integer k) { engine.getView().showMessage("Please login first."); }
    @Override public void onRequestHistory() { engine.getView().showMessage("Please login first."); }

    @Override public void printHelp() { engine.getView().showMessage(HELP_MESSAGE); }
    @Override public String getPrompt() { return "> "; }
    @Override public void onExit() {}
}
