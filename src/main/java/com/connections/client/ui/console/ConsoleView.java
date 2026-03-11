package com.connections.client.ui.console;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.models.WordGroup;
import com.connections.common.protocol.server_responses.ServerResponse;
import java.util.List;

/**
 * CLI implementation of ClientView. Uses synchronized blocks to ensure atomic output.
 */
public class ConsoleView implements ClientView {

    private static final int MS_TO_S_RATIO = 1000;
    private static final int MAX_MISTAKES = 4;
    private final Object consoleLock = new Object();

    @Override
    public void showResponse(ServerResponse<?> response) {
        synchronized (consoleLock) {
            System.out.println("[" + response.getStatus() + "] " + response.getMessage());
        }
    }

    @Override
    public void showGameInfo(GameInfoDTO info) {
        synchronized (consoleLock) {
            if (info.isFinished()) {
                System.out.println("Game is over. Use 'history' or 'info' for details.");
                return;
            }
            System.out.println("--- Game Status ---");
            System.out.println("Time remaining: " + (info.getRemainingTime() / MS_TO_S_RATIO) + "s");
            System.out.println(String.format("Mistakes: %d/%d", info.getMistakes(), MAX_MISTAKES));
            
            if (info.getFoundGroups() != null && !info.getFoundGroups().isEmpty()) {
                System.out.println("Groups found:");
                for (WordGroup group : info.getFoundGroups()) {
                    System.out.println("  - " + group.getTheme() + ": " + String.join(", ", group.getWords()));
                }
            }
            
            if (info.getRemainingWords() != null) {
                System.out.println("Remaining words: " + String.join(", ", info.getRemainingWords()));
            }
            System.out.println("Current score: " + info.getScore());
        }
    }

    @Override
    public void showGameStats(GameStatsDTO stats) {
        synchronized (consoleLock) {
            System.out.println("--- Global Game Stats ---");
            System.out.println("Active players: " + stats.getActivePlayers());
            System.out.println("Players finished: " + stats.getFinishedPlayers());
            System.out.println("Winners: " + stats.getWonPlayers());
            if (stats.getAverageScore() > 0) {
                System.out.println("Average score: " + stats.getAverageScore());
            }
        }
    }

    @Override
    public void showPlayerStats(PlayerStatsDTO stats) {
        synchronized (consoleLock) {
            System.out.println("--- Your Global Statistics ---");
            System.out.println("Puzzles completed: " + stats.getPuzzlesCompleted());
            System.out.println("Win rate: " + String.format("%.2f%%", stats.getWinRate()));
            System.out.println("Total score: " + stats.getTotalScore());
            System.out.println("Average score: " + stats.getAverageScore());
        }
    }

    @Override
    public void showLeaderboard(LeaderboardDTO leaderboard) {
        synchronized (consoleLock) {
            System.out.println("--- Global Leaderboard ---");
            List<LeaderboardEntryDTO> entries = leaderboard.getEntries();
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntryDTO entry = entries.get(i);
                System.out.printf("%2d. %-15s | Score: %5d\n", (i + 1), entry.getUsername(), entry.getScore());
            }
        }
    }

    @Override
    public void showNotification(String message) {
        synchronized (consoleLock) {
            System.out.println("\n[NOTIFICATION] " + message);
            System.out.print("> "); // Quick prompt restore
        }
    }

    @Override
    public void showError(String error) {
        synchronized (consoleLock) {
            System.err.println("Error: " + error);
        }
    }

    @Override
    public void showMessage(String message) {
        synchronized (consoleLock) {
            System.out.println(message);
        }
    }

    @Override
    public void renderPrompt(String prompt) {
        synchronized (consoleLock) {
            System.out.print(prompt);
        }
    }
}
