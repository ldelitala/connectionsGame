package com.connections.client;

import com.connections.client.modules.*;
import com.connections.client.controller_state.*;
import com.connections.client.view.ClientView;
import com.connections.client.view.CommandProvider;

/**
 * The Brain of the client. It no longer contains the loop.
 * It simply reacts to commands received from a CommandProvider.
 */
public class Controller implements CommandProvider.CommandListener {

    private static final String CMD_EXIT = "exit";
    private static final String CMD_HELP = "help";
    private static final String REGEX_WHITESPACE = "\\s+";

    private static final String SYSTEM_HELP = 
        "\nSystem:\n" +
        "  help              : Show this list\n" +
        "  exit              : Close the application\n";

    private final TcpConnectionPort tcpPort;
    private final int udpPort;
    private final ClientView view;
    private final CommandProvider commandProvider;
    
    private final UserModule userModule;
    private final GameModule gameModule;
    private final HistoryModule historyModule;

    private ControllerState currentState;

    public Controller(TcpConnectionPort tcpPort, int udpPort, ClientView view, CommandProvider provider) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.view = view;
        this.commandProvider = provider;
        this.userModule = new UserModule();
        this.gameModule = new GameModule();
        this.historyModule = new HistoryModule();
        this.currentState = new LoggedOutState(this);
    }

    /**
     * Starts the command flow by showing the initial prompt and 
     * connecting the provider to this listener.
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

    public void changeState(ControllerState newState) {
        this.currentState = newState;
    }

    public TcpConnectionPort getTcpPort() { return tcpPort; }
    public int getUdpPort() { return udpPort; }
    public ClientView getView() { return view; }
    public UserModule getUserModule() { return userModule; }
    public GameModule getGameModule() { return gameModule; }
    public HistoryModule getHistoryModule() { return historyModule; }
}
