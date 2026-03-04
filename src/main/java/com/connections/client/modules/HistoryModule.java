package com.connections.client.modules;

import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import com.connections.common.dto.*;
import com.connections.common.models.WordGroup;
import java.util.List;

/**
 * Handles operations related to past games and global statistics.
 */
public class HistoryModule implements ClientModule {

    @Override
    public Request parseCommand(String command, String[] tokens) {
        switch (command.toLowerCase()) {
            case "info":
                return (tokens.length > 1) ? HistoryRequest.getGameInfo(Integer.parseInt(tokens[1])) : null;
            case "stats":
                return (tokens.length > 1) ? HistoryRequest.getGameStats(Integer.parseInt(tokens[1])) : null;
            case "ranking":
                Integer top = (tokens.length > 1) ? Integer.parseInt(tokens[1]) : 10;
                String player = (tokens.length > 2) ? tokens[2] : null;
                return HistoryRequest.getLeaderboard(player, top);
            case "history":
                return HistoryRequest.getPlayerStats();
            default:
                return null;
        }
    }

    @Override
    public void displayResponse(Response<?> response) {
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
        if (!info.isFinished()) {
            System.out.println("Questa partita è ancora attiva. Usa 'info' senza ID per lo stato in diretta.");
            return;
        }
        System.out.println("--- Dettagli Partita Conclusa (ID: " + info.getGameId() + ") ---");
        System.out.println("Punteggio finale: " + info.getScore());
        System.out.println("Errori finali: " + info.getMistakes());
        System.out.println("Gruppi indovinati: " + info.getCorrectGroupsCount() + "/4");
        System.out.println("Assegnazioni ufficiali:");
        for (WordGroup group : info.getAllGroups()) {
            System.out.println("  - " + group.getCategory() + ": " + String.join(", ", group.getWords()));
        }
    }

    private void displayConcludedGameStats(GameStatsDTO stats) {
        System.out.println("--- Statistiche Globali Partita Conclusa ---");
        System.out.println("Partecipanti totali: " + stats.getTotalParticipants());
        System.out.println("Giocatori che hanno concluso: " + stats.getFinishedPlayers());
        System.out.println("Vincitori totali: " + stats.getWonPlayers());
        System.out.println("Punteggio medio: " + stats.getAverageScore());
    }

    private void displayPlayerStats(PlayerStatsDTO stats) {
        System.out.println("--- Le Tue Statistiche Globali ---");
        System.out.println("Partite giocate: " + stats.getTotalGamesPlayed());
        System.out.println("Partite concluse: " + stats.getGamesFinished());
        System.out.println("Partite vinte: " + stats.getGamesWon());
        System.out.println("Punteggio totale: " + stats.getTotalScore());
        System.out.println("Media errori per partita: " + stats.getAverageMistakes());
    }

    private void displayLeaderboard(LeaderboardDTO leaderboard) {
        System.out.println("--- Classifica ---");
        List<LeaderboardEntryDTO> entries = leaderboard.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDTO entry = entries.get(i);
            System.out.printf("%2d. %-15s | Punteggio: %5d\n", (i + 1), entry.getUsername(), entry.getScore());
        }
    }
}
