package com.connections.client;

import com.connections.client.modules.*;
import com.connections.client.controller_state.*;
import java.util.Scanner;

/**
 * Context for the Client State Pattern. Manages the current state of the application and provides
 * shared resources to individual states. It coordinates the main command loop and handles global
 * system commands like exit and help.
 */
public class Controller {

    // --- Commands ---
    private static final String CMD_EXIT = "exit";
    private static final String CMD_HELP = "help";

    // --- Help Messages ---
    private static final String SYSTEM_HELP =
            "\nSystem:\n" + "  help              : Show this list\n"
                    + "  exit              : Close the application\n";

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
        this.currentState = new LoggedOutState(this);
    }

    /**
     * Starts the main command loop. Reads input from the user, handles global system commands, and
     * delegates specific logic to the current state.
     */
    public void start() {
        while (true) {
            System.out.print(currentState.getPrompt());

            String line = scanner.nextLine().trim();
            if (line.isEmpty())
                continue;

            // Handle System-level commands globally
            if (line.equalsIgnoreCase(CMD_EXIT)) {
                System.out.println("Uscita in corso...");
                currentState.onExit();
                break;
            }

            if (line.equalsIgnoreCase(CMD_HELP)) {
                currentState.printHelp(); // 1. Print state-specific help
                System.out.println(SYSTEM_HELP); // 2. Append system help
                continue;
            }

            String[] tokens = line.split(REGEX_WHITESPACE);
            currentState.handleCommand(tokens);
        }
    }

    public void changeState(ControllerState newState) {
        this.currentState = newState;
    }

    // --- GETTERS FOR SHARED RESOURCES ---

    public TcpConnectionPort getTcpPort() {
        return tcpPort;
    }

    public UserModuleView getUserModule() {
        return userModule;
    }

    public GameModuleView getGameModule() {
        return gameModule;
    }

    public HistoryModuleView getHistoryModule() {
        return historyModule;
    }
}
