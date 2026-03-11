package com.connections.client.ui.console;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.models.Category;
import com.connections.common.protocol.server_responses.ServerResponse;
import java.util.List;

/**
 * CLI implementation of ClientView. 
 * Methods are synchronized to ensure atomic console output, preventing 
 * interleaved messages between TCP responses and asynchronous UDP notifications.
 */
public class ConsoleView implements ClientView {

    private static final int MS_TO_S_RATIO = 1000;
    private static final int MAX_MISTAKES = 4;

    @Override
    public synchronized void showResponse(ServerResponse<?> response) {
        System.out.println("[" + response.getStatus() + "] " + response.getMessage());
    }

    @Override
    public synchronized void showGameInfo(PuzzleStateDTO info) {
        if (info.isFinished()) {
            System.out.println("Puzzle is solved or expired. Use 'history' or 'info' for details.");
            return;
        }
        System.out.println("--- Puzzle Board ---");
        System.out.println("Time remaining: " + (info.getRemainingTime() / MS_TO_S_RATIO) + "s");
        System.out.println(String.format("Mistakes made: %d/%d", info.getMistakes(), MAX_MISTAKES));
        
        if (info.getFoundGroups() != null && !info.getFoundGroups().isEmpty()) {
            System.out.println("Categories found:");
            for (Category category : info.getFoundGroups()) {
                System.out.println("  - " + category.getTheme() + ": " + String.join(", ", category.getWords()));
            }
        }
        
        if (info.getRemainingWords() != null && !info.getRemainingWords().isEmpty()) {
            System.out.println("Remaining words: " + String.join(", ", info.getRemainingWords()));
        }
        System.out.println("Current score: " + info.getScore());
    }

    @Override
    public synchronized void showGameStats(PuzzleStatsDTO stats) {
        System.out.println("--- Global Puzzle Statistics ---");
        System.out.println("Active players: " + stats.getActivePlayers());
        System.out.println("Players finished: " + stats.getFinishedPlayers());
        System.out.println("Winners: " + stats.getWonPlayers());
        if (stats.getAverageScore() > 0) {
            System.out.println("Average score: " + stats.getAverageScore());
        }
    }

    @Override
    public synchronized void showPlayerStats(PlayerStatsDTO stats) {
        System.out.println("--- Your Career Statistics ---");
        System.out.println("Puzzles completed: " + stats.getPuzzlesCompleted());
        System.out.println("Win rate: " + String.format("%.2f%%", stats.getWinRate()));
        System.out.println("Total score: " + stats.getTotalScore());
        System.out.println("Average score: " + stats.getAverageScore());
    }

    @Override
    public synchronized void showLeaderboard(LeaderboardDTO leaderboard) {
        System.out.println("--- Global Leaderboard ---");
        List<LeaderboardEntryDTO> entries = leaderboard.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDTO entry = entries.get(i);
            System.out.printf("%2d. %-15s | Score: %5d\n", (i + 1), entry.getUsername(), entry.getScore());
        }
    }

    @Override
    public synchronized void showNotification(String message) {
        System.out.println("\n[NOTIFICATION] " + message);
        System.out.print("> "); 
    }

    @Override
    public synchronized void showError(String error) {
        System.err.println("Error: " + error);
    }

    @Override
    public synchronized void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public synchronized void renderPrompt(String prompt) {
        System.out.print(prompt);
    }
}
