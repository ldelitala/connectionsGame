package com.connections.client.state;

import com.connections.client.ClientController;
import com.connections.client.modules.ModuleView;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.Operation;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * State representing an authenticated user.
 * Allows playing, viewing history, and logging out.
 */
public class LoggedInState implements ClientState {
    private final ClientController controller;
    private final String username;

    public LoggedInState(ClientController controller, String username) {
        this.controller = controller;
        this.username = username;
    }

    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;
        ModuleView activeModule = null;

        switch (command) {
            case "logout": 
                request = controller.getUserModule().prepareLogout(); 
                activeModule = controller.getUserModule();
                break;
            case "update": 
                request = controller.getUserModule().prepareUpdate(username); 
                activeModule = controller.getUserModule();
                break;
            case "submit": 
                request = controller.getGameModule().prepareSubmit(tokens); 
                activeModule = controller.getGameModule();
                break;
            case "status": 
                request = controller.getGameModule().prepareStatus(); 
                activeModule = controller.getGameModule();
                break;
            case "stats":  
                request = controller.getGameModule().prepareStats(); 
                activeModule = controller.getGameModule();
                break;
            case "info":   
                request = controller.getHistoryModule().preparePastInfo(tokens); 
                activeModule = controller.getHistoryModule();
                break;
            case "global": 
                request = controller.getHistoryModule().prepareGlobalStats(tokens); 
                activeModule = controller.getHistoryModule();
                break;
            case "ranking": 
                request = controller.getHistoryModule().prepareRanking(tokens); 
                activeModule = controller.getHistoryModule();
                break;
            case "history": 
                request = controller.getHistoryModule().prepareHistory(); 
                activeModule = controller.getHistoryModule();
                break;
            case "register":
            case "login":
                System.out.println("Sei già loggato come [" + username + "]. Fai logout prima.");
                return;
            default:
                System.out.println("Comando non riconosciuto.");
                return;
        }

        if (request != null) {
            executeAndDisplay(request, activeModule);
        }
    }

    private void executeAndDisplay(ClientRequest request, ModuleView activeModule) {
        try {
            ServerResponse<?> response = controller.getTcpPort().sendRequest(request);
            
            // Clean polymorphic call
            activeModule.displayResponse(response);

            if (response.getStatus() == Status.OK && request.getOperation() == Operation.LOGOUT) {
                controller.changeState(new LoggedOutState(controller));
            }
        } catch (Exception e) {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    @Override
    public void printHelp() {
        System.out.println("\n--- COMANDI DISPONIBILI (Loggato) ---");
        System.out.println("  submit [w1-w4] / status / stats   : Gioco attuale");
        System.out.println("  info [id] / global [id] / ranking : Storico e Classifiche");
        System.out.println("  history                           : Statistiche personali");
        System.out.println("  update / logout                   : Gestione account");
        System.out.println("  exit                              : Chiudi l'applicazione\n");
    }

    @Override
    public String getPrompt() {
        return "[" + username + "] > ";
    }
}
