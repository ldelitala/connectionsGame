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

    /**
     * Initializes a new GameEngine with necessary infrastructure components.
     *
     * @param gateway The network gateway for server communication.
     * @param udpPort The local UDP port used for incoming notifications.
     * @param view The UI component for displaying information.
     * @param inputProvider The source of user input.
     */
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

    /**
     * Starts the game engine's main execution loop.
     * Displays the initial prompt and starts the input listener.
     */
    public void run() {
        view.renderPrompt(currentState.getPrompt());
        inputProvider.startListening(this);
    }

    /**
     * Processes input strings received from the {@link InputProvider}.
     * Handles system-level commands like 'help' and 'exit' before delegating
     * state-specific commands to the current {@link EngineState}.
     *
     * @param data The raw input line from the user.
     */
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

    /**
     * Changes the current active state of the game engine.
     *
     * @param newState The new {@link EngineState} to transition to.
     */
    public void changeState(EngineState newState) {
        this.currentState = newState;
    }

    /** @return The network gateway used by the engine. */
    public NetworkGateway getGateway() { return gateway; }
    
    /** @return The local UDP port used for server notifications. */
    public int getUdpPort() { return udpPort; }
    
    /** @return The UI view interface. */
    public ClientView getView() { return view; }
    
    /** @return The service for identity and account management. */
    public IdentityService getIdentityService() { return identityService; }
    
    /** @return The service for active game logic and status. */
    public GameplayService getGameplayService() { return gameplayService; }
    
    /** @return The service for historical data and rankings. */
    public ArchiveService getArchiveService() { return archiveService; }
}
