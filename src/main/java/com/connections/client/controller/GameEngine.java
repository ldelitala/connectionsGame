package com.connections.client.controller;

import com.connections.client.network.NetworkGateway;
import com.connections.client.services.*;
import com.connections.client.controller.state.*;
import com.connections.client.ui.ClientView;
import com.connections.client.ui.InputProvider;

/**
 * The Game Engine: The central reactive hub of the application.
 * It operates on a "Command Protocol": it receives normalized command strings
 * and delegates their execution to the current EngineState.
 */
public class GameEngine implements InputProvider.InputListener {

    private static final String CMD_EXIT = "exit";
    private static final String CMD_HELP = "help";
    private static final String REGEX_WHITESPACE = "\\s+";

    private static final String SYSTEM_HELP = 
        "\nSystem:\n" +
        "  help              : Show this list\n" +
        "  exit              : Close the application\n";

    private final NetworkGateway gateway;
    private final int udpPort;
    private final ClientView view;
    private final InputProvider inputProvider;
    
    private final IdentityService identityService;
    private final GameplayService gameplayService;
    private final ArchiveService archiveService;

    private EngineState currentState;

    public GameEngine(NetworkGateway gateway, int udpPort, ClientView view, InputProvider inputProvider) {
        this.gateway = gateway;
        this.udpPort = udpPort;
        this.view = view;
        this.inputProvider = inputProvider;
        this.identityService = new IdentityService(view);
        this.gameplayService = new GameplayService(view);
        this.archiveService = new ArchiveService(view);
        this.currentState = new LoggedOutState(this);
    }

    public void run() {
        view.renderPrompt(currentState.getPrompt());
        inputProvider.startListening(this);
    }

    @Override
    public void onInput(String data) {
        String line = data.trim();
        if (line.isEmpty()) return;

        if (line.equalsIgnoreCase(CMD_EXIT)) {
            view.showMessage("Exiting Engine...");
            currentState.onExit();
            inputProvider.stop();
            return;
        }

        if (line.equalsIgnoreCase(CMD_HELP)) {
            currentState.printHelp();
            view.showMessage(SYSTEM_HELP);
        } else {
            String[] tokens = line.split(REGEX_WHITESPACE);
            currentState.handleCommand(tokens);
        }
        
        view.renderPrompt(currentState.getPrompt());
    }

    public void changeState(EngineState newState) {
        this.currentState = newState;
    }

    public NetworkGateway getGateway() { return gateway; }
    public int getUdpPort() { return udpPort; }
    public ClientView getView() { return view; }
    public IdentityService getIdentityService() { return identityService; }
    public GameplayService getGameplayService() { return gameplayService; }
    public ArchiveService getArchiveService() { return archiveService; }
}
