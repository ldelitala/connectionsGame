package com.connections.client.state;

import java.io.IOException;
import com.connections.client.ClientController;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * Rappresenta lo stato del client quando l'utente non è ancora autenticato (Logged Out).
 * In questo stato, le uniche operazioni permesse sono la registrazione e il login.
 * Implementa il pattern State per gestire il comportamento contestuale del client.
 */
public class LoggedOutState implements ClientState {
    
    /** Il controller principale che agisce da contesto per lo stato. */
    private final ClientController controller;
    
    /** Flag interno per tracciare se l'ultima operazione richiesta è stata un tentativo di login. */
    private boolean tryLogin = false;

    /**
     * Costruisce un nuovo stato LoggedOut.
     * 
     * @param controller il controller che gestisce la macchina a stati del client.
     */
    public LoggedOutState(ClientController controller) {
        this.controller = controller;
    }

    /**
     * Gestisce i comandi dell'utente filtrandoli per lo stato disconnesso.
     * Supporta i comandi 'register' e 'login'. Se un comando non è supportato in questo
     * stato, informa l'utente della necessità di autenticarsi.
     * 
     * @param tokens l'array di stringhe che compone il comando digitato dall'utente.
     */
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
                tryLogin = true;
                break;

            default:
                System.out.println("Comando non disponibile. Effettua il login per giocare.");
                return;
        }

        if (request != null) {
            try {
                // Invia la richiesta al server tramite il porto TCP
                ServerResponse<?> response = controller.getTcpPort().sendRequest(request);
                
                // Visualizza l'esito dell'operazione tramite il modulo utente
                controller.getUserModule().displayResponse(response);

                // Se il login ha avuto successo, transiziona allo stato LoggedIn
                if (tryLogin && response.getStatus() == Status.OK) {
                    // tokens[1] contiene lo username usato per il login
                    controller.changeState(new LoggedInState(controller, tokens[1]));
                    tryLogin = false; 
                }

            } catch (IOException e) {
                System.err.println("Errore di comunicazione: " + e.getMessage());
            }
        }
    }

    /**
     * Stampa l'aiuto contestuale per lo stato di utente disconnesso.
     * Mostra solo i comandi di registrazione, accesso e uscita dal sistema.
     */
    @Override
    public void printHelp() {
        System.out.println("\n--- COMANDI DISPONIBILI (Disconnesso) ---");
        System.out.println("  register [u] [p]  : Registra un nuovo account");
        System.out.println("  login [u] [p]     : Accedi al gioco");
        System.out.println("  help              : Mostra questo elenco");
        System.out.println("  exit              : Chiudi l'applicazione\n");
    }

    /**
     * Restituisce il prompt dei comandi base per l'utente non loggato.
     * 
     * @return la stringa del prompt "> ".
     */
    @Override
    public String getPrompt() {
        return "> ";
    }
}
