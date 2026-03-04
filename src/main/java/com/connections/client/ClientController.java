package com.connections.client;

import com.connections.client.modules.*;
import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import java.io.IOException;
import java.util.Scanner;

/**
 * Orchestrates the CLI interaction by delegating to specialized modules.
 * Manages TCP communication and maintains local session state.
 */
public class ClientController {
    private final ConnectionClient tcpClient;
    private final UserModule userModule;
    private final GameModule gameModule;
    private final HistoryModule historyModule;
    
    private String loggedUser = null;

    public ClientController(ConnectionClient tcpClient, Scanner scanner, int udpPort) {
        this.tcpClient = tcpClient;
        this.userModule = new UserModule(scanner, udpPort);
        this.gameModule = new GameModule();
        this.historyModule = new HistoryModule();
    }

    /**
     * Starts the main command loop.
     */
    public void start(Scanner scanner) {
        while (true) {
            String prompt = (loggedUser != null) ? "[" + loggedUser + "] > " : "> ";
            System.out.print(prompt);
            
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }
            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            String[] tokens = line.split("\s+");
            String command = tokens[0].toLowerCase();

            // 1. Identify which module should handle this command
            ClientModule module = route(command, tokens);
            if (module == null) {
                System.out.println("Unknown command or invalid arguments. Type 'help' for usage.");
                continue;
            }

            // 2. Module parses the command into a Request
            Request request = module.parseCommand(command, tokens);
            if (request == null) {
                // Module already printed error message
                continue;
            }

            // 3. Send request via TCP
            try {
                Response<?> response = tcpClient.sendRequest(request);
                
                // 4. Update session state if necessary (login/logout/update)
                updateSessionState(request, response, tokens);
                
                // 5. Let the module display the response
                module.displayResponse(response);
                
            } catch (IOException e) {
                System.err.println("Network error: " + e.getMessage());
            }
        }
    }

    private ClientModule route(String command, String[] tokens) {
        // Simple routing logic based on command name
        switch (command) {
            case "register":
            case "login":
            case "logout":
            case "update":
                return userModule;
            case "submit":
                return gameModule;
            case "info":
            case "stats":
                // If it has arguments, it's historical (ID), otherwise it's current
                return (tokens.length > 1) ? historyModule : gameModule;
            case "ranking":
            case "history":
                return historyModule;
            default:
                return null;
        }
    }

    private void updateSessionState(Request request, Response<?> response, String[] tokens) {
        if (response.getStatus() != Status.OK) return;

        Operation op = request.getOperation();
        if (op == Operation.LOGIN) {
            // Get username from tokens if available, or from request (deserialized)
            // For simplicity, we assume login was successful if status is OK
            if (tokens.length >= 2) {
                this.loggedUser = tokens[1];
            }
        } else if (op == Operation.LOGOUT) {
            this.loggedUser = null;
        } else if (op == Operation.UPDATE_CREDENTIALS) {
            // If username was updated, we should ideally get it from the response
            // but for now let's just keep the old one or ask for a refresh
        }
    }

    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("--- Account ---");
        System.out.println("  register [user] [pass]     - Create a new account");
        System.out.println("  login [user] [pass]        - Sign in");
        System.out.println("  logout                     - Sign out");
        System.out.println("  update                     - Change username or password");
        System.out.println("--- Active Game ---");
        System.out.println("  submit [w1] [w2] [w3] [w4] - Submit 4 words");
        System.out.println("  info                       - View current game status");
        System.out.println("  stats                      - View current game statistics");
        System.out.println("--- Historical & Global ---");
        System.out.println("  info [id]                  - View details of a past game");
        System.out.println("  stats [id]                 - View global stats of a past game");
        System.out.println("  ranking [k]                - View top K players");
        System.out.println("  history                    - View your personal statistics");
        System.out.println("--- App ---");
        System.out.println("  help                       - Show this help message");
        System.out.println("  exit                       - Close the application");
    }
}
