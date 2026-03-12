package com.connections.client.engine;

import com.connections.client.network.NetworkGateway;
import com.connections.client.engine.modules.*;
import com.connections.client.engine.state.*;
import com.connections.client.ui.ClientView;
import com.connections.client.ui.InputProvider;
import java.util.List;

/**
 * The Game Engine: The central reactive hub of the application.
 * It reacts to explicit actions received from an InputProvider.
 */
public class GameEngine implements InputProvider.InputListener {

    private static final String SYSTEM_HELP = 
        "\nSystem:\n" +
        "  help              : Show this list\n" +
        "  exit              : Close the application\n";

    private final NetworkGateway gateway;
    private final int udpPort;
    private final ClientView view;
    private final InputProvider inputProvider;
    
    private final UserModule userModule;
    private final GameplayModule gameplayModule;
    private final HistoryModule historyModule;

    private EngineState currentState;

    public GameEngine(NetworkGateway gateway, int udpPort, ClientView view, InputProvider inputProvider) {
        this.gateway = gateway;
        this.udpPort = udpPort;
        this.view = view;
        this.inputProvider = inputProvider;
        this.userModule = new UserModule(view, gateway);
        this.gameplayModule = new GameplayModule(view, gateway);
        this.historyModule = new HistoryModule(view, gateway);
        this.currentState = new LoggedOutState(this);
    }

    public void run() {
        view.renderPrompt(currentState.getPrompt());
        inputProvider.startListening(this);
    }

    // --- Action Implementations (Delegating to State) ---

    @Override
    public void onRegister(String username, String password) {
        currentState.onRegister(username, password);
        postAction();
    }

    @Override
    public void onLogin(String username, String password) {
        currentState.onLogin(username, password);
        postAction();
    }

    @Override
    public void onLogout() {
        currentState.onLogout();
        postAction();
    }

    @Override
    public void onUpdateCredentials(String oldPass, String newName, String newPass) {
        currentState.onUpdateCredentials(oldPass, newName, newPass);
        postAction();
    }

    @Override
    public void onGuess(List<String> words) {
        currentState.onGuess(words);
        postAction();
    }

    @Override
    public void onRequestBoard() {
        currentState.onRequestBoard();
        postAction();
    }

    @Override
    public void onRequestStats() {
        currentState.onRequestStats();
        postAction();
    }

    @Override
    public void onRequestPastInfo(int puzzleId) {
        currentState.onRequestPastInfo(puzzleId);
        postAction();
    }

    @Override
    public void onRequestGlobalStats(int puzzleId) {
        currentState.onRequestGlobalStats(puzzleId);
        postAction();
    }

    @Override
    public void onRequestRanking(String username, Integer topK) {
        currentState.onRequestRanking(username, topK);
        postAction();
    }

    @Override
    public void onRequestHistory() {
        currentState.onRequestHistory();
        postAction();
    }

    @Override
    public void onHelp() {
        currentState.printHelp();
        view.showMessage(SYSTEM_HELP);
        postAction();
    }

    @Override
    public void onExit() {
        view.showMessage("Exiting Engine...");
        currentState.onExit();
        inputProvider.stop();
    }

    private void postAction() {
        view.renderPrompt(currentState.getPrompt());
    }

    public void changeState(EngineState newState) {
        this.currentState = newState;
    }

    // --- Getters for Modules and Infrastructure ---
    public NetworkGateway getGateway() { return gateway; }
    public int getUdpPort() { return udpPort; }
    public ClientView getView() { return view; }
    public UserModule getUserModule() { return userModule; }
    public GameplayModule getGameplayModule() { return gameplayModule; }
    public HistoryModule getHistoryModule() { return historyModule; }
}
