package com.connections.client;

import com.connections.client.modules.*;
import com.connections.client.controller_state.*;
import com.connections.client.view.ClientView;
import java.util.Scanner;

/**
 * Context for the Client State Pattern (Controller).
 */
public class Controller {

    private static final String CMD_EXIT = "exit";
    private static final String CMD_HELP = "help";
    private static final String REGEX_WHITESPACE = "\\s+";

    private static final String SYSTEM_HELP = 
        "\nSystem:\n" +
        "  help              : Show this list\n" +
        "  exit              : Close the application\n";

    private final TcpConnectionPort tcpPort;
    private final Scanner scanner;
    private final int udpPort;
    private final ClientView view;
    
    private final UserModule userModule;
    private final GameModule gameModule;
    private final HistoryModule historyModule;

    private ControllerState currentState;

    public Controller(TcpConnectionPort tcpPort, Scanner scanner, int udpPort, ClientView view) {
        this.tcpPort = tcpPort;
        this.scanner = scanner;
        this.udpPort = udpPort;
        this.view = view;
        this.userModule = new UserModule();
        this.gameModule = new GameModule();
        this.historyModule = new HistoryModule();
        this.currentState = new LoggedOutState(this);
    }

    public void start() {
        while (true) {
            view.renderPrompt(currentState.getPrompt());

            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (line.equalsIgnoreCase(CMD_EXIT)) {
                view.showMessage("Exiting...");
                currentState.onExit();
                break;
            }

            if (line.equalsIgnoreCase(CMD_HELP)) {
                currentState.printHelp();
                view.showMessage(SYSTEM_HELP);
                continue;
            }

            String[] tokens = line.split(REGEX_WHITESPACE);
            currentState.handleCommand(tokens);
        }
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
