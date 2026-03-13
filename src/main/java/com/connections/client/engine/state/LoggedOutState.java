package com.connections.client.engine.state;

import com.connections.client.engine.GameEngine;
import com.connections.common.protocol.client_requests.UserRequest;
import com.connections.common.protocol.server_responses.Status;
import java.util.List;

/**
 * State for players who are not authenticated.
 */
public class LoggedOutState implements EngineState {

    private static final String PLEASE_LOGIN = "Please log in to perform this action.";

    private final GameEngine engine;


    public LoggedOutState(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onRegister(String username, String password) {
        engine.getExecutor().execute(UserRequest.register(username, password));
    }

    @Override
    public void onLogin(String username, String password) {
        Status status = engine.getExecutor().execute(UserRequest.login(username, password, engine.getUdpPort()));
        if (status == Status.OK) {
            engine.changeState(new LoggedInState(engine, username));
        }
    }

    @Override
    public void onLogout() {
        engine.getView().showMessage(PLEASE_LOGIN);
    }

    @Override
    public void onUpdateCredentials(String oP, String nU, String nP) {
        engine.getView().showMessage(PLEASE_LOGIN);
    }

    @Override
    public void onGuess(List<String> words) {
        engine.getView().showMessage(PLEASE_LOGIN);
    }

    @Override
    public void onRequestBoard() {
        engine.getView().showMessage(PLEASE_LOGIN);
    }

    @Override
    public void onRequestStats() {
        engine.getView().showMessage(PLEASE_LOGIN);
    }

    @Override
    public void onRequestPastInfo(int id) {
        engine.getView().showMessage(PLEASE_LOGIN);
    }

    @Override
    public void onRequestGlobalStats(int id) {
        engine.getView().showMessage(PLEASE_LOGIN);
    }

    @Override
    public void onRequestRanking(String u, Integer k) {
        engine.getView().showMessage(PLEASE_LOGIN);
    }

    @Override
    public void onRequestHistory() {
        engine.getView().showMessage(PLEASE_LOGIN);
    }

    @Override
    public String getPrompt() {
        return "> ";
    }

    @Override
    public void onExit() {}

    @Override
    public void onHelp() {
        engine.getView().showHelp(false);
    }
}
