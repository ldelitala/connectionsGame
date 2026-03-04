package com.connections.client.modules;

import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import com.connections.common.dto.*;
import com.connections.common.models.WordGroup;
import java.util.Arrays;
import java.util.List;

/**
 * Handles operations and display for the active game session.
 */
public class GameModule {

    public Request prepareSubmit(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Uso: submit parola1 parola2 parola3 parola4");
            return null;
        }
        List<String> words = Arrays.asList(Arrays.copyOfRange(tokens, 1, tokens.length));
        return GameRequest.submitProposal(words);
    }

    public Request prepareStatus() {
        return GameRequest.getCurrentInfo();
    }

    public Request prepareStats() {
        return GameRequest.getCurrentStats();
    }

    public void displayResponse(Response<?> response) {
        System.out.println(response.getMessage());
        Object data = response.getData();
        if (data == null) return;

        if (data instanceof GameInfoDTO) {
            displayActiveGameInfo((GameInfoDTO) data);
        } else if (data instanceof GameStatsDTO) {
            displayActiveGameStats((GameStatsDTO) data);
        }
    }

    private void displayActiveGameInfo(GameInfoDTO info) {
        if (info.isFinished()) {
            System.out.println("La tua partita per questo puzzle è conclusa. Usa 'history' per i dettagli.");
            return;
        }
        System.out.println("--- Stato Partita Corrente ---");
        System.out.println("Tempo rimasto: " + (info.getRemainingTime() / 1000) + "s");
        System.out.println("Errori: " + info.getMistakes() + "/4");
        System.out.println("Gruppi trovati:");
        for (WordGroup group : info.getFoundGroups()) {
            System.out.println("  - " + group.getCategory() + ": " + String.join(", ", group.getWords()));
        }
        System.out.println("Parole rimaste: " + String.join(", ", info.getRemainingWords()));
        System.out.println("Punteggio attuale: " + info.getScore());
    }

    private void displayActiveGameStats(GameStatsDTO stats) {
        System.out.println("--- Statistiche Globali Partita Corrente ---");
        System.out.println("Giocatori attivi: " + stats.getActivePlayers());
        System.out.println("Giocatori che hanno concluso: " + stats.getFinishedPlayers());
        System.out.println("Vincitori: " + stats.getWonPlayers());
    }
}
