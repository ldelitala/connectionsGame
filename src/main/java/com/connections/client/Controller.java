package com.connections.client;

import com.connections.client.modules.*;
import com.connections.client.controller_state.*;
import java.util.Scanner;

/**
 * Context for the Client State Pattern.
 * Manages the current state of the application and provides shared resources to individual states.
 * It coordinates the main command loop and handles global system commands like exit and help.
 */
public class Controller {

    // --- Commands ---
    private static final String CMD_EXIT = "exit";
    private static final String CMD_HELP = "help";

    // --- Help Messages ---
    // @formatter:off
    private static final String SYSTEM_HELP = 
        "\nSystem:\n" +
        "  help              : Show this list\n" +
        "  exit              : Close the application\n";
    // @formatter:on

    // --- Patterns ---
    private static final String REGEX_WHITESPACE = "\\s+";

    private final TcpConnectionPort tcpPort;
    private final Scanner scanner;
    private final UserModuleView userModule;
    private final GameModuleView gameModule;
    private final HistoryModuleView historyModule;

    private ControllerState currentState;

    /**
     * Constructs a new Controller and initializes the modules and the initial state.
     * 
     * @param tcpPort the TCP port used for server communication.
     * @param scanner the scanner for reading user input.
     * @param udpPort the port for receiving asynchronous UDP notifications.
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
     * Starts the main command loop. Reads input from the user, handles global system
     * commands, and delegates specific logic to the current state.
     */
    public void start() {
        while (true) {
            System.out.print(currentState.getPrompt());

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            // Handle System-level commands globally
            if (line.equalsIgnoreCase(CMD_EXIT)) {
                System.out.println("Exiting...");
                currentState.onExit();
                break;
            }

            if (line.equalsIgnoreCase(CMD_HELP)) {
                printFullHelp();
                continue;
            }

            String[] tokens = line.split(REGEX_WHITESPACE);
            // Delegate command handling to the current state
            currentState.handleCommand(tokens);
        }
    }

    /**
     * Prints the full help documentation, including state-specific commands and
     * global system commands.
     */
    private void printFullHelp() {
        // 1. Print state-specific help
        currentState.printHelp();
        // 2. Append system help
        System.out.println(SYSTEM_HELP);
    }

    /**
     * Changes the current state of the controller.
     * 
     * @param newState the new state to transition to.
     */
    public void changeState(ControllerState newState) {
        this.currentState = newState;
    }

    // --- Getters for states to access common resources ---

    /**
     * Returns the TCP connection port.
     * @return the TcpConnectionPort instance.
     */
    public TcpConnectionPort getTcpPort() { return tcpPort; }

    /**
     * Returns the user management module view.
     * @return the UserModuleView instance.
     */
    public UserModuleView getUserModule() { return userModule; }

    /**
     * Returns the active game module view.
     * @return the GameModuleView instance.
     */
    public GameModuleView getGameModule() { return gameModule; }

    /**
     * Returns the historical data module view.
     * @return the HistoryModuleView instance.
     */
    public HistoryModuleView getHistoryModule() { return historyModule; }
}
