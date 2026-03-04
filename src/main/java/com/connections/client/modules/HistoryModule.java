package com.connections.client.modules;

import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.common.dto.*;
import com.connections.common.models.WordGroup;
import java.util.List;

/**
 * Handles operations and display for past games and global stats.
 */
public class HistoryModule {

    public ClientRequest preparePastInfo(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Uso: info [id]");
            return null;
        }
        return HistoryRequest.getGameInfo(Integer.parseInt(tokens[1]));
    }

    public ClientRequest prepareGlobalStats(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Uso: global [id]");
            return null;
        }
        return HistoryRequest.getGameStats(Integer.parseInt(tokens[1]));
    }

    public ClientRequest prepareRanking(String[] tokens) {
        Integer top = (tokens.length > 1) ? Integer.parseInt(tokens[1]) : 10;
        String player = (tokens.length > 2) ? tokens[2] : null;
        return HistoryRequest.getLeaderboard(player, top);
    }

    public ClientRequest prepareHistory() {
        return HistoryRequest.getPlayerStats();
    }

    public void displayResponse(ServerResponse<?> response) {
        System.out.println(response.getMessage());
        Object data = response.getData();
        if (data == null) return;

        if (data instanceof GameInfoDTO) {
            displayConcludedGameInfo((GameInfoDTO) data);
        } else if (data instanceof GameStatsDTO) {
            displayConcludedGameStats((GameStatsDTO) data);
        } else if (data instanceof PlayerStatsDTO) {
            displayPlayerStats((PlayerStatsDTO) data);
        } else if (data instanceof LeaderboardDTO) {
            displayLeaderboard((LeaderboardDTO) data);
        }
    }

    private void displayConcludedGameInfo(GameInfoDTO info) {
        System.out.println("--- Dettagli Partita Conclusa (ID: " + info.getGameId() + ") ---");
        System.out.println("Punteggio finale: " + info.getScore());
        System.out.println("Errori commessi: " + info.getMistakes());
        System.out.println("Gruppi indovinati: " + info.getCorrectGroupsCount() + "/4");
        System.out.println("Assegnazioni ufficiali:");
        for (WordGroup group : info.getAllGroups()) {
            System.out.println("  - " + group.getTheme() + ": " + String.join(", ", group.getWords()));
        }
    }

    private void displayConcludedGameStats(GameStatsDTO stats) {
        System.out.println("--- Statistiche Partita Passata (ID: " + stats.getGameId() + ") ---");
        System.out.println("Partecipanti totali: " + stats.getTotalParticipants());
        System.out.println("Hanno concluso: " + stats.getFinishedPlayers());
        System.out.println("Vincitori totali: " + stats.getWonPlayers());
        System.out.println("Punteggio medio: " + stats.getAverageScore());
    }

    private void displayPlayerStats(PlayerStatsDTO stats) {
        System.out.println("--- Le Tue Statistiche Globali ---");
        System.out.println("Puzzles completati: " + stats.getPuzzlesCompleted());
        System.out.println("Puzzles vinti: " + (int)(stats.getWinRate() * stats.getPuzzlesCompleted() / 100));
        System.out.println("Punteggio totale: " + stats.getTotalScore());
        System.out.println("Media punteggio: " + stats.getAverageScore());
    }

    private void displayLeaderboard(LeaderboardDTO leaderboard) {
        System.out.println("--- Classifica Globale ---");
        List<LeaderboardEntryDTO> entries = leaderboard.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDTO entry = entries.get(i);
            System.out.printf("%2d. %-15s | Punteggio: %5d\n", (i + 1), entry.getUsername(), entry.getScore());
        }
    }
}
