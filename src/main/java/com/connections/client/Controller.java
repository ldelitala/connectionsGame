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

    // --- Help Messages ---
    private static final String SYSTEM_HELP = 
        "\nSystem:\n" +
        "  help              : Show this list\n" +
        "  exit              : Close the application\n";

    // --- Patterns ---
    private static final String REGEX_WHITESPACE = "\\s+";

    private final TcpConnectionPort tcpPort;
    private final Scanner scanner;
    private final UserModuleView userModule;
    private final GameModuleView gameModule;
    private final HistoryModuleView historyModule;

    private ControllerState currentState;

    public Controller(TcpConnectionPort tcpPort, Scanner scanner, int udpPort) {
        this.tcpPort = tcpPort;
        this.scanner = scanner;
        this.userModule = new UserModuleView(scanner, udpPort);
        this.gameModule = new GameModuleView();
        this.historyModule = new HistoryModuleView();
        this.currentState = new LoggedOutState(this);
    }

    public void start() {
        while (true) {
            System.out.print(currentState.getPrompt());

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            // Handle System-level commands globally
            if (line.equalsIgnoreCase(CMD_EXIT)) {
                System.out.println("Uscita in corso...");
                break;
            }

            if (line.equalsIgnoreCase(CMD_HELP)) {
                printFullHelp();
                continue;
            }

            String[] tokens = line.split(REGEX_WHITESPACE);
            currentState.handleCommand(tokens);
        }
    }

    private void printFullHelp() {
        // 1. Print state-specific help
        currentState.printHelp();
        // 2. Append system help
        System.out.println(SYSTEM_HELP);
    }

    public void changeState(ControllerState newState) {
        this.currentState = newState;
    }

    public TcpConnectionPort getTcpPort() { return tcpPort; }
    public UserModuleView getUserModule() { return userModule; }
    public GameModuleView getGameModule() { return gameModule; }
    public HistoryModuleView getHistoryModule() { return historyModule; }
}
