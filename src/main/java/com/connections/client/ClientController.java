package com.connections.client;

import com.connections.client.modules.*;
import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import java.io.IOException;
import java.util.Scanner;

/**
 * Main handler for client commands.
 * It parses the input and delegates the request creation and display to specific modules.
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

    public void start(Scanner scanner) {
        while (true) {
            String prompt = (loggedUser != null) ? "[" + loggedUser + "] > " : "> ";
            System.out.print(prompt);
            
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            
            if (line.equalsIgnoreCase("exit")) {
                break;
            }
            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            String[] tokens = line.split("\\s+");
            String command = tokens[0].toLowerCase();

            Request request = null;
            Object activeModule = null;

            // PARSING UNIFICATO NEL CONTROLLER
            switch (command) {
                // USER MODULE
                case "register":
                    request = userModule.prepareRegister(tokens);
                    activeModule = userModule;
                    break;
                case "login":
                    request = userModule.prepareLogin(tokens);
                    activeModule = userModule;
                    break;
                case "logout":
                    request = userModule.prepareLogout();
                    activeModule = userModule;
                    break;
                case "update":
                    request = userModule.prepareUpdate();
                    activeModule = userModule;
                    break;

                // GAME MODULE
                case "submit":
                    request = gameModule.prepareSubmit(tokens);
                    activeModule = gameModule;
                    break;
                
                // INFO & STATS (Routing condizionale)
                case "info":
                    if (tokens.length > 1) {
                        request = historyModule.prepareGameInfo(tokens);
                        activeModule = historyModule;
                    } else {
                        request = gameModule.prepareInfo();
                        activeModule = gameModule;
                    }
                    break;
                case "stats":
                    if (tokens.length > 1) {
                        request = historyModule.prepareGameStats(tokens);
                        activeModule = historyModule;
                    } else {
                        request = gameModule.prepareStats();
                        activeModule = gameModule;
                    }
                    break;

                // HISTORY MODULE
                case "ranking":
                    request = historyModule.prepareRanking(tokens);
                    activeModule = historyModule;
                    break;
                case "history":
                    request = historyModule.prepareHistory();
                    activeModule = historyModule;
                    break;

                default:
                    System.out.println("Comando sconosciuto. Digita 'help' per la lista comandi.");
            }

            // ESECUZIONE E VISUALIZZAZIONE
            if (request != null) {
                try {
                    Response<?> response = tcpClient.sendRequest(request);
                    updateSessionState(request, response, tokens);
                    
                    // Delega la visualizzazione al modulo che ha preparato la richiesta
                    if (activeModule instanceof UserModule) ((UserModule) activeModule).displayResponse(response);
                    else if (activeModule instanceof GameModule) ((GameModule) activeModule).displayResponse(response);
                    else if (activeModule instanceof HistoryModule) ((HistoryModule) activeModule).displayResponse(response);

                } catch (IOException e) {
                    System.err.println("Errore di rete: " + e.getMessage());
                }
            }
        }
    }

    private void updateSessionState(Request request, Response<?> response, String[] tokens) {
        if (response.getStatus() != Status.OK) return;

        Operation op = request.getOperation();
        if (op == Operation.LOGIN) {
            if (tokens.length >= 2) {
                this.loggedUser = tokens[1];
                this.userModule.setCurrentUser(this.loggedUser);
            }
        } else if (op == Operation.LOGOUT) {
            this.loggedUser = null;
            this.userModule.setCurrentUser(null);
        }
    }

    private void printHelp() {
        System.out.println("--- Account ---");
        System.out.println("  register [user] [pass]  / login [user] [pass] / logout / update");
        System.out.println("--- Gioco ---");
        System.out.println("  submit [w1] [w2] [w3] [w4] / info / stats");
        System.out.println("--- Storico ---");
        System.out.println("  info [id] / stats [id] / ranking [k] [user] / history");
    }
}
