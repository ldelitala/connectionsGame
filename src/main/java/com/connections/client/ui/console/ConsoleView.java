package com.connections.client.ui.console;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.models.Category;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.notifications.*;
import java.util.List;

/**
 * CLI implementation of ClientView. 
 * Methods are synchronized to ensure atomic console output.
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
    public synchronized void showNotification(Notification notification) {
        System.out.println("\n[NOTIFICATION] Type: " + notification.getType());
        
        if (notification instanceof NewGameNotification) {
            displayNewGame((NewGameNotification) notification);
        } else if (notification instanceof GameEndNotification) {
            displayGameEnd((GameEndNotification) notification);
        } else if (notification instanceof PlayerGameSummaryNotification) {
            displayPlayerSummary((PlayerGameSummaryNotification) notification);
        }
        
        System.out.print("> "); 
    }

    private void displayNewGame(NewGameNotification n) {
        System.out.println("A new Puzzle has started! ID: " + n.getGameId());
        System.out.println("Words: " + String.join(", ", n.getWords()));
        System.out.println("Type 'board' to start playing.");
    }

    private void displayGameEnd(GameEndNotification n) {
        System.out.println("The current Puzzle has ended.");
        if (n.getStats() != null) {
            showGameStats(n.getStats());
        }
        if (n.getLeaderboard() != null) {
            showLeaderboard(n.getLeaderboard());
        }
    }

    private void displayPlayerSummary(PlayerGameSummaryNotification n) {
        System.out.println("Your personal summary for the concluded puzzle:");
        if (n.getPersonalInfo() != null) {
            showGameInfo(n.getPersonalInfo());
        }
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
