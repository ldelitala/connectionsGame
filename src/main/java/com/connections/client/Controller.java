package com.connections.client;

import com.connections.client.modules.*;
import com.connections.client.controller_state.*;
import java.util.Scanner;

/**
 * Context for the Client State Pattern.
 * Manages the current state and provides shared resources to states.
 */
public class Controller {

    // --- Commands ---
    private static final String CMD_EXIT = "exit";
    private static final String CMD_HELP = "help";

    // --- Patterns ---
    private static final String REGEX_WHITESPACE = "\\s+";

    // --- Messages ---
    private static final String MSG_EXITING = "Uscita in corso...";

    private final TcpConnectionPort tcpPort;
    private final Scanner scanner;
    private final UserModuleView userModule;
    private final GameModuleView gameModule;
    private final HistoryModuleView historyModule;

    private ControllerState currentState;

    /**
     * Constructs a new Controller with the given network resources and scanner.
     * 
     * @param tcpPort the TCP connection port.
     * @param scanner the shared scanner for user input.
     * @param udpPort the port for UDP notifications.
     */
    public Controller(TcpConnectionPort tcpPort, Scanner scanner, int udpPort) {
        this.tcpPort = tcpPort;
        this.scanner = scanner;
        this.userModule = new UserModuleView(scanner, udpPort);
        this.gameModule = new GameModuleView();
        this.historyModule = new HistoryModuleView();
        // Initial state
        this.currentState = new LoggedOutState(this);
    }

    /**
     * Starts the main command loop.
     */
    public void start() {
        while (true) {
            System.out.print(currentState.getPrompt());

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.equalsIgnoreCase(CMD_EXIT)) {
                System.out.println(MSG_EXITING);
                break;
            }

            if (line.equalsIgnoreCase(CMD_HELP)) {
                currentState.printHelp();
                continue;
            }

            String[] tokens = line.split(REGEX_WHITESPACE);
            // Delegate command handling to the current state
            currentState.handleCommand(tokens);
        }
    }

    /**
     * Changes the current state of the controller.
     * 
     * @param newState the new state to transition to.
     */
    public void changeState(ControllerState newState) {
        this.currentState = newState;
    }

    // --- Getters ---
    public TcpConnectionPort getTcpPort() { return tcpPort; }
    public UserModuleView getUserModule() { return userModule; }
    public GameModuleView getGameModule() { return gameModule; }
    public HistoryModuleView getHistoryModule() { return historyModule; }
}
