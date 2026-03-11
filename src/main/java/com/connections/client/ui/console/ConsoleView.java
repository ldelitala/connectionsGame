package com.connections.client.ui.console;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.models.Category;
import com.connections.common.protocol.server_responses.ServerResponse;
import java.util.List;

/**
 * CLI implementation of ClientView. Uses synchronized blocks to ensure atomic output.
 */
public class ConsoleView implements ClientView {

    private static final int MS_TO_S_RATIO = 1000;
    private static final int MAX_MISTAKES = 4;
    private final Object consoleLock = new Object();

    /**
     * Prints the server response status and message to the standard output.
     *
     * @param response The response received from the server.
     */
    @Override
    public void showResponse(ServerResponse<?> response) {
        synchronized (consoleLock) {
            System.out.println("[" + response.getStatus() + "] " + response.getMessage());
        }
    }

    /**
     * Renders a textual representation of the active puzzle board, including remaining time,
     * mistakes, found categories, and remaining words.
     *
     * @param info DTO containing the puzzle state.
     */
    @Override
    public void showGameInfo(PuzzleStateDTO info) {
        synchronized (consoleLock) {
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
    }

    /**
     * Prints global statistics for a puzzle (active players, winners, average score).
     *
     * @param stats DTO containing puzzle statistics.
     */
    @Override
    public void showGameStats(PuzzleStatsDTO stats) {
        synchronized (consoleLock) {
            System.out.println("--- Global Puzzle Statistics ---");
            System.out.println("Active players: " + stats.getActivePlayers());
            System.out.println("Players finished: " + stats.getFinishedPlayers());
            System.out.println("Winners: " + stats.getWonPlayers());
            if (stats.getAverageScore() > 0) {
                System.out.println("Average score: " + stats.getAverageScore());
            }
        }
    }

    /**
     * Prints the career statistics of the current player.
     *
     * @param stats DTO containing player statistics.
     */
    @Override
    public void showPlayerStats(PlayerStatsDTO stats) {
        synchronized (consoleLock) {
            System.out.println("--- Your Career Statistics ---");
            System.out.println("Puzzles completed: " + stats.getPuzzlesCompleted());
            System.out.println("Win rate: " + String.format("%.2f%%", stats.getWinRate()));
            System.out.println("Total score: " + stats.getTotalScore());
            System.out.println("Average score: " + stats.getAverageScore());
        }
    }

    /**
     * Prints a formatted leaderboard table.
     *
     * @param leaderboard DTO containing leaderboard entries.
     */
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

    /**
     * Prints a notification message with a prefix and resets the cursor to the prompt.
     *
     * @param message The notification message.
     */
    @Override
    public void showNotification(String message) {
        synchronized (consoleLock) {
            System.out.println("\n[NOTIFICATION] " + message);
            System.out.print("> "); 
        }
    }

    /**
     * Prints an error description to the standard error stream.
     *
     * @param error The error message.
     */
    @Override
    public void showError(String error) {
        synchronized (consoleLock) {
            System.err.println("Error: " + error);
        }
    }

    /**
     * Prints a message to the standard output.
     *
     * @param message The message text.
     */
    @Override
    public void showMessage(String message) {
        synchronized (consoleLock) {
            System.out.println(message);
        }
    }

    /**
     * Prints the command prompt without a trailing newline.
     *
     * @param prompt The prompt string.
     */
    @Override
    public void renderPrompt(String prompt) {
        synchronized (consoleLock) {
            System.out.print(prompt);
        }
    }
}
