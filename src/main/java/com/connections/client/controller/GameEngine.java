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

    // --- System Commands (Global Lifecycle) ---
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
     * Initializes the engine and starts listening for inputs from the provider.
     */
    public void run() {
        view.renderPrompt(currentState.getPrompt());
        inputProvider.startListening(this);
    }

    /**
     * Processes a normalized game command.
     * This is the entry point for all user interactions, regardless of the UI (CLI/GUI).
     */
    @Override
    public void onInput(String data) {
        String line = data.trim();
        if (line.isEmpty()) return;

        // Handle Global System Commands
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
            // Tokenize and delegate to the current state
            String[] tokens = line.split(REGEX_WHITESPACE);
            currentState.handleCommand(tokens);
        }
        
        // Signal the view that we are ready for the next interaction
        view.renderPrompt(currentState.getPrompt());
    }

    public void changeState(EngineState newState) {
        this.currentState = newState;
    }

    // Getters for services used by states...
    public NetworkGateway getGateway() { return gateway; }
    public int getUdpPort() { return udpPort; }
    public ClientView getView() { return view; }
    public IdentityService getIdentityService() { return identityService; }
    public GameplayService getGameplayService() { return gameplayService; }
    public ArchiveService getArchiveService() { return archiveService; }
}
