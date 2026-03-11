package com.connections.client.controller;

import com.connections.client.network.NetworkGateway;
import com.connections.client.services.*;
import com.connections.client.controller.state.*;
import com.connections.client.ui.ClientView;
import com.connections.client.ui.CommandProvider;

/**
 * The Brain of the client. It no longer contains the loop.
 * It simply reacts to commands received from a CommandProvider.
 */
public class GameEngine implements CommandProvider.CommandListener {

    private static final String CMD_EXIT = "exit";
    private static final String CMD_HELP = "help";
    private static final String REGEX_WHITESPACE = "\\s+";

    private static final String SYSTEM_HELP = 
        "\nSystem:\n" +
        "  help              : Show this list\n" +
        "  exit              : Close the application\n";

    private final NetworkGateway tcpPort;
    private final int udpPort;
    private final ClientView view;
    private final CommandProvider commandProvider;
    
    private final IdentityService identityService;
    private final GameplayService gameplayService;
    private final ArchiveService archiveService;

    private EngineState currentState;

    public GameEngine(NetworkGateway tcpPort, int udpPort, ClientView view, CommandProvider provider) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.view = view;
        this.commandProvider = provider;
        this.identityService = new IdentityService();
        this.gameplayService = new GameplayService();
        this.archiveService = new ArchiveService();
        this.currentState = new LoggedOutState(this);
    }

    /**
     * Starts the engine flow.
     */
    public void run() {
        view.renderPrompt(currentState.getPrompt());
        commandProvider.startProviding(this);
    }

    /**
     * Callback method called every time a new command is received.
     */
    @Override
    public void onCommand(String line) {
        if (line.equalsIgnoreCase(CMD_EXIT)) {
            view.showMessage("Exiting...");
            currentState.onExit();
            commandProvider.stop();
            return;
        }

        if (line.equalsIgnoreCase(CMD_HELP)) {
            currentState.printHelp();
            view.showMessage(SYSTEM_HELP);
        } else {
            String[] tokens = line.split(REGEX_WHITESPACE);
            currentState.handleCommand(tokens);
        }
        
        // After every command, render the next prompt
        view.renderPrompt(currentState.getPrompt());
    }

    public void changeState(EngineState newState) {
        this.currentState = newState;
    }

    public NetworkGateway getTcpPort() { return tcpPort; }
    public int getUdpPort() { return udpPort; }
    public ClientView getView() { return view; }
    public IdentityService getIdentityService() { return identityService; }
    public GameplayService getGameplayService() { return gameplayService; }
    public ArchiveService getArchiveService() { return archiveService; }
}
