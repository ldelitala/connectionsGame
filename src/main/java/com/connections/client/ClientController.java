package com.connections.client;

import com.connections.client.modules.*;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import java.io.IOException;
import java.util.Scanner;

/**
 * Main handler for client commands.
 * Orchestrates parsing and delegates execution to specialized modules.
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
            if (line.equalsIgnoreCase("exit")) break;
            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            String[] tokens = line.split("\\s+");
            
            // Logica estratta in una funzione a parte
            CommandResult result = processCommand(tokens);

            // ESECUZIONE
            if (result != null && result.request != null) {
                try {
                    ServerResponse<?> response = tcpClient.sendRequest(result.request);
                    updateSessionState(result.request, response, tokens);
                    
                    // Delega la visualizzazione al modulo corretto
                    result.displayResponse(response);

                } catch (IOException e) {
                    System.err.println("Errore di connessione: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Analizza il comando e lo smista al modulo corretto.
     * Restituisce un oggetto contenente la richiesta e il modulo di riferimento.
     */
    private CommandResult processCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            // ACCOUNT
            case "register": return new CommandResult(userModule.prepareRegister(tokens), userModule);
            case "login":    return new CommandResult(userModule.prepareLogin(tokens), userModule);
            case "logout":   return new CommandResult(userModule.prepareLogout(), userModule);
            case "update":   return new CommandResult(userModule.prepareUpdate(), userModule);

            // GIOCO ATTIVO
            case "submit":   return new CommandResult(gameModule.prepareSubmit(tokens), gameModule);
            case "status":   return new CommandResult(gameModule.prepareStatus(), gameModule);
            case "stats":    return new CommandResult(gameModule.prepareStats(), gameModule);

            // STORICO E GLOBALE
            case "info":     return new CommandResult(historyModule.preparePastInfo(tokens), historyModule);
            case "global":   return new CommandResult(historyModule.prepareGlobalStats(tokens), historyModule);
            case "ranking":  return new CommandResult(historyModule.prepareRanking(tokens), historyModule);
            case "history":  return new CommandResult(historyModule.prepareHistory(), historyModule);

            default:
                System.out.println("Comando non riconosciuto. Scrivi 'help' per vedere la lista dei comandi.");
                return null;
        }
    }

    private void updateSessionState(ClientRequest request, ServerResponse<?> response, String[] tokens) {
        if (response.getStatus() != Status.OK) return;
        Operation op = request.getOperation();
        if (op == Operation.LOGIN && tokens.length >= 2) {
            this.loggedUser = tokens[1];
            this.userModule.setCurrentUser(this.loggedUser);
        } else if (op == Operation.LOGOUT) {
            this.loggedUser = null;
            this.userModule.setCurrentUser(null);
        }
    }

    private void printHelp() {
        System.out.println("\n--- COMANDI DISPONIBILI ---");
        System.out.println("Gestione Account:");
        System.out.println("  register [u] [p]  : Registra un nuovo account");
        System.out.println("  login [u] [p]     : Accedi al gioco");
        System.out.println("  logout            : Esci dal tuo account");
        System.out.println("  update            : Cambia le tue credenziali (username/password)");
        
        System.out.println("\nPartita Corrente (In Corso):");
        System.out.println("  submit [w1-w4]    : Invia una proposta di 4 parole");
        System.out.println("  status            : Visualizza il tuo stato attuale (errori, parole, tempo)");
        System.out.println("  stats             : Statistiche globali sulla partita in corso");
        
        System.out.println("\nStorico e Classifiche:");
        System.out.println("  info [id]         : Guarda la soluzione e i tuoi dettagli di una partita passata");
        System.out.println("  global [id]       : Guarda le statistiche di partecipazione di una partita passata");
        System.out.println("  ranking [k] [u]   : Mostra i primi K giocatori o la posizione di un utente");
        System.out.println("  history           : Visualizza il riepilogo delle tue statistiche personali");
        
        System.out.println("\nSistema:");
        System.out.println("  help              : Mostra questo elenco");
        System.out.println("  exit              : Chiudi l'applicazione\n");
    }

    /**
     * Classe di supporto interna per accoppiare una Request al modulo che deve gestirne la risposta.
     */
    private static class CommandResult {
        final ClientRequest request;
        final Object targetModule; // UserModule, GameModule o HistoryModule

        CommandResult(ClientRequest request, Object targetModule) {
            this.request = request;
            this.targetModule = targetModule;
        }

        void displayResponse(ServerResponse<?> response) {
            if (targetModule instanceof UserModule) ((UserModule) targetModule).displayResponse(response);
            else if (targetModule instanceof GameModule) ((GameModule) targetModule).displayResponse(response);
            else if (targetModule instanceof HistoryModule) ((HistoryModule) targetModule).displayResponse(response);
        }
    }
}
