package com.connections.client;

import com.connections.client.modules.*;
import com.connections.client.state.*;
import java.util.Scanner;

/**
 * Context for the Client State Pattern.
 * Manages the current state and provides shared resources to states.
 */
public class ClientController {
    private final TcpConnectionPort tcpPort;
    private final UserModule userModule;
    private final GameModule gameModule;
    private final HistoryModule historyModule;
    
    private ClientState currentState;

    public ClientController(TcpConnectionPort tcpPort, Scanner scanner, int udpPort) {
        this.tcpPort = tcpPort;
        this.userModule = new UserModule(scanner, udpPort);
        this.gameModule = new GameModule();
        this.historyModule = new HistoryModule();
        // Initial state
        this.currentState = new LoggedOutState(this);
    }

    public void start(Scanner scanner) {
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
     * Changes the current state of the client.
     */
    public void changeState(ClientState newState) {
        this.currentState = newState;
    }

    // Getters for states to access common resources
    public TcpConnectionPort getTcpPort() { return tcpPort; }
    public UserModule getUserModule() { return userModule; }
    public GameModule getGameModule() { return gameModule; }
    public HistoryModule getHistoryModule() { return historyModule; }
}
