package com.connections.client.state;

import com.connections.client.ClientController;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.Operation;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * State representing a user who is not authenticated.
 * Allows only registration and login.
 */
public class LoggedOutState implements ClientState {
    private final ClientController controller;

    public LoggedOutState(ClientController controller) {
        this.controller = controller;
    }

    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;

        switch (command) {
            case "register":
                request = controller.getUserModule().prepareRegister(tokens);
                break;
            case "login":
                request = controller.getUserModule().prepareLogin(tokens);
                break;
            default:
                System.out.println("Comando non disponibile. Effettua il login per giocare.");
                return;
        }

        if (request != null) {
            executeAndTransition(request, tokens);
        }
    }

    private void executeAndTransition(ClientRequest request, String[] tokens) {
        try {
            ServerResponse<?> response = controller.getTcpPort().sendRequest(request);
            controller.getUserModule().displayResponse(response);

            if (response.getStatus() == Status.OK && request.getOperation() == Operation.LOGIN) {
                // Transition to LoggedInState
                String username = (tokens.length >= 2) ? tokens[1] : "User";
                controller.changeState(new LoggedInState(controller, username));
            }
        } catch (Exception e) {
            System.err.println("Errore: " + e.getMessage());
        }
    }

    @Override
    public void printHelp() {
        System.out.println("\n--- COMANDI DISPONIBILI (Disconnesso) ---");
        System.out.println("  register [u] [p]  : Registra un nuovo account");
        System.out.println("  login [u] [p]     : Accedi al gioco");
        System.out.println("  help              : Mostra questo elenco");
        System.out.println("  exit              : Chiudi l'applicazione\n");
    }

    @Override
    public String getPrompt() {
        return "> ";
    }
}
