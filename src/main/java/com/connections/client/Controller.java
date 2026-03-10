package com.connections.client;

import com.connections.client.modules.*;
import com.connections.client.state.*;
import java.util.Scanner;

/**
 * Context for the Client State Pattern.
 * Manages the current state and provides shared resources to states.
 */
public class Controller {
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
        // Initial state
        this.currentState = new LoggedOutState(this);
    }

    public void start() {
        while (true) {
            System.out.print(currentState.getPrompt());

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Uscita in corso...");
                break;
            }

            if (line.equalsIgnoreCase("help")) {
                currentState.printHelp();
                continue;
            }

            String[] tokens = line.split("\\s+");
            // Delegate command handling to the current state
            currentState.handleCommand(tokens);
        }
    }

    /**
     * Changes the current state of the controller.
     */
    public void changeState(ControllerState newState) {
        this.currentState = newState;
    }

    // Getters for states to access common resources
    public TcpConnectionPort getTcpPort() { return tcpPort; }
    public UserModuleView getUserModule() { return userModule; }
    public GameModuleView getGameModule() { return gameModule; }
    public HistoryModuleView getHistoryModule() { return historyModule; }
}
