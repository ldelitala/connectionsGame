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
            
            // Gestione del comando exit come richiesto
            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Uscita in corso...");
                break;
            }

            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            String[] tokens = line.split("\\s+");
            String command = tokens[0].toLowerCase();

            // 1. Identifica quale modulo deve gestire il comando (Routing)
            ClientModule module = route(command, tokens);
            if (module == null) {
                System.out.println("Comando sconosciuto o argomenti non validi. Digita 'help' per aiuto.");
                continue;
            }

            // 2. Il modulo prepara la richiesta (Parsing)
            Request request = module.parseCommand(command, tokens);
            if (request == null) {
                continue; // Il modulo ha già stampato l'errore se necessario
            }

            // 3. Il Controller invia la richiesta via TCP e aspetta la risposta
            try {
                Response<?> response = tcpClient.sendRequest(request);
                
                // 4. Aggiorna lo stato della sessione (login/logout/update) se l'operazione ha avuto successo
                updateSessionState(request, response, tokens);
                
                // 5. Il Controller ripassa la risposta al modulo per la visualizzazione
                module.displayResponse(response);
                
            } catch (IOException e) {
                System.err.println("Errore di rete: " + e.getMessage());
            }
        }
    }

    private ClientModule route(String command, String[] tokens) {
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
                // Se ha un argomento è storico (ID), altrimenti è partita corrente
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
            if (tokens.length >= 2) {
                this.loggedUser = tokens[1];
                this.userModule.setCurrentUser(this.loggedUser);
            }
        } else if (op == Operation.LOGOUT) {
            this.loggedUser = null;
            this.userModule.setCurrentUser(null);
        } else if (op == Operation.UPDATE_CREDENTIALS) {
            // Se l'utente ha cambiato username (tokens[1] in handleUpdate), andrebbe aggiornato
            // Per ora lasciamo che il login successivo o la logica del server lo gestiscano
        }
    }

    private void printHelp() {
        System.out.println("Comandi disponibili:");
        System.out.println("--- Account ---");
        System.out.println("  register [user] [pass]     - Crea un nuovo account");
        System.out.println("  login [user] [pass]        - Effettua il login");
        System.out.println("  logout                     - Effettua il logout");
        System.out.println("  update                     - Cambia username o password");
        System.out.println("--- Gioco Attivo ---");
        System.out.println("  submit [w1] [w2] [w3] [w4] - Invia una proposta di 4 parole");
        System.out.println("  info                       - Info sulla tua partita corrente");
        System.out.println("  stats                      - Statistiche globali sulla partita corrente");
        System.out.println("--- Storico e Classifica ---");
        System.out.println("  info [id]                  - Dettagli di una partita passata");
        System.out.println("  stats [id]                 - Statistiche di una partita passata");
        System.out.println("  ranking [k] [user]         - Top K giocatori o posizione di un utente");
        System.out.println("  history                    - Visualizza le tue statistiche personali");
        System.out.println("--- App ---");
        System.out.println("  help                       - Mostra questo messaggio");
        System.out.println("  exit                       - Chiudi l'applicazione");
    }
}
