package com.connections.client.modules;

import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.common.dto.*;
import com.connections.common.models.WordGroup;
import java.util.List;

/**
 * Handles operations and display for past games and global stats.
 */
public class HistoryModuleView implements ResponseView {

    // --- Logic Constants ---
    private static final int MIN_TOKENS_ARG = 2;
    private static final int INDEX_ARG_1 = 1;
    private static final int INDEX_ARG_2 = 2;
    private static final String FLAG_TOP = "-top";

    public ClientRequest preparePastInfo(String[] tokens) {
        if (tokens.length < MIN_TOKENS_ARG) {
            System.out.println("Usage: info [id]");
            return null;
        }
        return HistoryRequest.getGameInfo(Integer.parseInt(tokens[INDEX_ARG_1]));
    }

    public ClientRequest prepareGlobalStats(String[] tokens) {
        if (tokens.length < MIN_TOKENS_ARG) {
            System.out.println("Usage: global [id]");
            return null;
        }
        return HistoryRequest.getGameStats(Integer.parseInt(tokens[INDEX_ARG_1]));
    }

    public ClientRequest prepareRanking(String[] tokens) {
        if (tokens.length < MIN_TOKENS_ARG) {
            System.out.println("Usage:");
            System.out.println("  ranking <username>      : View a specific player's rank");
            System.out.println("  ranking -top [k]        : View top k players (omit k for full list)");
            return null;
        }

        String firstArg = tokens[INDEX_ARG_1];

        if (firstArg.equalsIgnoreCase(FLAG_TOP)) {
            // Case: ranking -top [k]
            Integer k = (tokens.length > INDEX_ARG_2) ? Integer.parseInt(tokens[INDEX_ARG_2]) : Integer.MAX_VALUE;
            return HistoryRequest.getLeaderboard(null, k);
        } else {
            // Case: ranking <username>
            return HistoryRequest.getLeaderboard(firstArg, null);
        }
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
        System.out.println("--- Past Game Details (ID: " + info.getGameId() + ") ---");
        System.out.println("Final score: " + info.getScore());
        System.out.println("Mistakes made: " + info.getMistakes());
        System.out.println("Groups found: " + info.getCorrectGroupsCount() + "/4");
        System.out.println("Official assignments:");
        for (WordGroup group : info.getAllGroups()) {
            System.out.println("  - " + group.getTheme() + ": " + String.join(", ", group.getWords()));
        }
    }

    private void displayConcludedGameStats(GameStatsDTO stats) {
        System.out.println("--- Past Game Global Stats (ID: " + stats.getGameId() + ") ---");
        System.out.println("Total participants: " + stats.getTotalParticipants());
        System.out.println("Players finished: " + stats.getFinishedPlayers());
        System.out.println("Total winners: " + stats.getWonPlayers());
        System.out.println("Average score: " + stats.getAverageScore());
    }

    private void displayPlayerStats(PlayerStatsDTO stats) {
        System.out.println("--- Your Global Statistics ---");
        System.out.println("Puzzles completed: " + stats.getPuzzlesCompleted());
        System.out.println("Puzzles won: " + (int) (stats.getWinRate() * stats.getPuzzlesCompleted() / 100));
        System.out.println("Total score: " + stats.getTotalScore());
        System.out.println("Average score: " + stats.getAverageScore());
    }

    private void displayLeaderboard(LeaderboardDTO leaderboard) {
        System.out.println("--- Global Leaderboard ---");
        List<LeaderboardEntryDTO> entries = leaderboard.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDTO entry = entries.get(i);
            System.out.printf("%2d. %-15s | Score: %5d\n", (i + 1), entry.getUsername(), entry.getScore());
        }
    }
}
