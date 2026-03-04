package com.connections.client;

import com.connections.client.modules.*;
import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
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
            String command = tokens[0].toLowerCase();

            Request request = null;
            Object activeModule = null;

            // PARSING LINEARE E DISACCOPPIATO
            switch (command) {
                // ACCOUNT
                case "register": request = userModule.prepareRegister(tokens); activeModule = userModule; break;
                case "login":    request = userModule.prepareLogin(tokens);    activeModule = userModule; break;
                case "logout":   request = userModule.prepareLogout();         activeModule = userModule; break;
                case "update":   request = userModule.prepareUpdate();         activeModule = userModule; break;

                // GIOCO ATTIVO (Sulla partita corrente)
                case "submit":   request = gameModule.prepareSubmit(tokens); activeModule = gameModule; break;
                case "status":   request = gameModule.prepareStatus();       activeModule = gameModule; break;
                case "stats":    request = gameModule.prepareStats();        activeModule = gameModule; break;

                // STORICO E GLOBALE (Su partite specifiche o classifiche)
                case "info":     request = historyModule.preparePastInfo(tokens);   activeModule = historyModule; break;
                case "global":   request = historyModule.prepareGlobalStats(tokens); activeModule = historyModule; break;
                case "ranking":  request = historyModule.prepareRanking(tokens);     activeModule = historyModule; break;
                case "history":  request = historyModule.prepareHistory();           activeModule = historyModule; break;

                default:
                    System.out.println("Comando non riconosciuto. Scrivi 'help' per vedere la lista dei comandi.");
            }

            // ESECUZIONE
            if (request != null) {
                try {
                    Response<?> response = tcpClient.sendRequest(request);
                    updateSessionState(request, response, tokens);
                    
                    // Delega la visualizzazione
                    if (activeModule instanceof UserModule) ((UserModule) activeModule).displayResponse(response);
                    else if (activeModule instanceof GameModule) ((GameModule) activeModule).displayResponse(response);
                    else if (activeModule instanceof HistoryModule) ((HistoryModule) activeModule).displayResponse(response);

                } catch (IOException e) {
                    System.err.println("Errore di connessione: " + e.getMessage());
                }
            }
        }
    }

    private void updateSessionState(Request request, Response<?> response, String[] tokens) {
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
}
