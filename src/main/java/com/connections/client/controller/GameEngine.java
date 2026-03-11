package com.connections.client.controller;

import com.connections.client.network.NetworkGateway;
import com.connections.client.services.*;
import com.connections.client.controller.state.*;
import com.connections.client.ui.ClientView;
import com.connections.client.ui.InputProvider;

/**
 * The Brain of the client (Game Engine).
 * It reacts to inputs received from an InputProvider.
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
        this.identityService = new IdentityService();
        this.gameplayService = new GameplayService();
        this.archiveService = new ArchiveService();
        this.currentState = new LoggedOutState(this);
    }

    /**
     * Starts the Game Engine flow.
     */
    public void run() {
        view.renderPrompt(currentState.getPrompt());
        inputProvider.startListening(this);
    }

    /**
     * Callback method called every time a new input is received from the provider.
     */
    @Override
    public void onInput(String line) {
        if (line.equalsIgnoreCase(CMD_EXIT)) {
            view.showMessage("Exiting...");
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
        
        // After processing input, render the next prompt
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
