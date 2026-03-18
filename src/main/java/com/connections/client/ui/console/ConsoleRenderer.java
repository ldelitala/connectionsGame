package com.connections.client.ui.console;

import com.connections.common.protocol.server_responses.dto.*;
import com.connections.common.notifications.*;
import java.util.List;

/**
 * Static painters for CLI rendering. Separates the 'how' from the 'what'.
 */
public class ConsoleRenderer {

    private static final int MS_TO_S_RATIO = 1000;
    private static final int MAX_MISTAKES = 4;

    public static void paintGuessResult(GuessResultDTO result) {
        if ("success".equals(result.getStatus())) {
            System.out.println("Correct! +" + result.getPointsDelta() + " points.");
        } else {
            System.out.println("Wrong! " + result.getPointsDelta() + " points.");
        }
        System.out.println("Remaining words: " + String.join(", ", result.getRemainingWords()));
        System.out.println("New total score: " + result.getTotalPoints());
    }

    public static void paintPuzzleInfo(PuzzleStateDTO info) {
        if (info.isFinished()) {
            System.out.println("--- Past Puzzle Board (ID: " + info.getPuzzleId() + ") ---");
            System.out.println("Final Score: " + info.getScore());
            System.out.println("Final Mistakes: " + info.getMistakes() + "/" + MAX_MISTAKES);
            
            if (info.getAllGroups() != null && !info.getAllGroups().isEmpty()) {
                System.out.println("Puzzle Solution:");
                for (com.connections.common.models.Category cat : info.getAllGroups()) {
                    System.out.println("  [" + cat.getTheme() + "]: " + String.join(", ", cat.getWords()));
                }
            } else if (info.getFoundGroups() != null && !info.getFoundGroups().isEmpty()) {
                System.out.println("Categories you found:");
                for (String categoryName : info.getFoundGroups()) {
                    System.out.println("  - " + categoryName);
                }
            }
            return;
        }
        System.out.println("--- Puzzle Board ---");
        if (info.getRemainingTime() != null) {
            System.out.println("Time remaining: " + (info.getRemainingTime() / MS_TO_S_RATIO) + "s");
        }
        System.out.println(String.format("Mistakes made: %d/%d", info.getMistakes(), MAX_MISTAKES));

        if (info.getFoundGroups() != null && !info.getFoundGroups().isEmpty()) {
            System.out.println("Categories found:");
            for (String categoryName : info.getFoundGroups()) {
                System.out.println("  - " + categoryName);
            }
        }

        if (info.getRemainingWords() != null && !info.getRemainingWords().isEmpty()) {
            System.out.println("Remaining words: " + String.join(", ", info.getRemainingWords()));
        }
        System.out.println("Current score: " + info.getScore());
    }

    public static void paintPuzzleStats(PuzzleStatsDTO stats) {
        System.out.println("--- Global Puzzle Statistics ---");
        System.out.println("Active players: " + (stats.getActivePlayers() != null ? stats.getActivePlayers() : 0));
        System.out.println("Players finished: " + (stats.getFinishedPlayers() != null ? stats.getFinishedPlayers() : 0));
        System.out.println("Winners: " + (stats.getWonPlayers() != null ? stats.getWonPlayers() : 0));
        if (stats.getAverageScore() != null && stats.getAverageScore() > 0) {
            System.out.println("Average score: " + stats.getAverageScore());
        }
    }

    public static void paintPlayerStats(PlayerStatsDTO stats) {
        System.out.println("--- Your Career Statistics ---");
        System.out.println("Puzzles completed: " + stats.getPuzzlesCompleted());
        System.out.println("Win rate: " + String.format("%.2f%%", stats.getWinRate()));
        System.out.println("Total score: " + stats.getTotalScore());
        System.out.println("Average score: " + stats.getAverageScore());
    }

    public static void paintLeaderboard(LeaderboardDTO leaderboard) {
        System.out.println("--- Global Leaderboard ---");
        List<LeaderboardEntryDTO> entries = leaderboard.getEntries();
        if (entries == null || entries.isEmpty()) {
            System.out.println("No entries found in leaderboard.");
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDTO entry = entries.get(i);
            System.out.printf("%2d. %-15s | Score: %5d\n", (i + 1), entry.getUsername(),
                    entry.getScore());
        }
    }

    public static void paintRank(LeaderboardEntryDTO e) {
        System.out.println("Your rank: " + e.getRank() + " | Username: " + e.getUsername()
                + " | Score: " + e.getScore());
    }

    // --- Notification Painters ---

    public static void paintNewPuzzleNotify(NewPuzzleNotification n) {
        System.out.println("\n[NOTIFICATION] A new Puzzle has started! ID: " + n.getPuzzleId());
        System.out.println("Words: " + String.join(", ", n.getWords()));
        System.out.println("Type 'board' to view the puzzle board.");
    }

    public static void paintPuzzleEndNotify(PuzzleEndNotification n) {
        System.out.println("\n[NOTIFICATION] The current Puzzle has ended.");
        if (n.getStats() != null)
            paintPuzzleStats(n.getStats());
        if (n.getLeaderboard() != null)
            paintLeaderboard(n.getLeaderboard());
    }

    public static void paintPlayerSummaryNotify(PlayerPuzzleSummaryNotification n) {
        System.out.println("\n[NOTIFICATION] Personal summary for the concluded puzzle:");
        if (n.getPersonalInfo() != null)
            paintPuzzleInfo(n.getPersonalInfo());
    }
}
