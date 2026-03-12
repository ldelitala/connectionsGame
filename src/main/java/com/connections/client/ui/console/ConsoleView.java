package com.connections.client.ui.console;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.models.Category;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.notifications.*;
import java.util.List;

/**
 * CLI implementation of ClientView. Methods are synchronized to ensure atomic console output.
 */
public class ConsoleView implements ClientView {

    private static final String HELP_SYSTEM =
            "\n--- SYSTEM COMMANDS ---\n" +
            "  help              : Show this list\n" +
            "  exit              : Logout and close the application\n";

    private static final String HELP_AUTH =
            "\n--- AUTHENTICATION ---\n" +
            "  register <uP> <psw>: Create a new account\n" +
            "  login <uP> <psw>   : Sign in to your account\n";

    private static final String HELP_GAMEPLAY =
            "\n--- GAMEPLAY COMMANDS ---\n" +
            "  board             : Show current puzzle board\n" +
            "  guess <w1 w2 w3 w4>: Submit a proposal (exactly 4 words)\n" +
            "  stats             : View global stats for current puzzle\n" +
            "\n--- HISTORY & RANKING ---\n" +
            "  history           : View your career statistics\n" +
            "  ranking <username>: View a specific player's current rank\n" +
            "  ranking -top [k]  : View the top K players (k is optional)\n" +
            "  info <id>         : View past puzzle board by ID\n" +
            "  global <id>       : View past puzzle global stats by ID\n" +
            "\n--- ACCOUNT ---\n" +
            "  update <old> <nU> <nP>: Update credentials (use '-' to skip username or password)\n" +
            "  logout            : Sign out of the session\n";

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
                System.out.println("  - " + category.getTheme() + ": "
                        + String.join(", ", category.getWords()));
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
            System.out.printf("%2d. %-15s | Score: %5d\n", (i + 1), entry.getUsername(),
                    entry.getScore());
        }
    }

    @Override
    public synchronized void showHelp(boolean loggedIn) {
        System.out.print(HELP_SYSTEM);
        System.out.print(loggedIn ? HELP_GAMEPLAY : HELP_AUTH);
        System.out.println();
    }

    @Override
    public synchronized void onNewPuzzle(NewGameNotification n) {
        System.out.println("\n[NOTIFICATION] A new Puzzle has started! ID: " + n.getGameId());
        System.out.println("Words: " + String.join(", ", n.getWords()));
        System.out.println("Type 'board' to view the game state.");
        System.out.print("> ");
    }

    @Override
    public synchronized void onPuzzleEnd(GameEndNotification n) {
        System.out.println("\n[NOTIFICATION] The current Puzzle has ended.");
        if (n.getStats() != null) {
            showGameStats(n.getStats());
        }
        if (n.getLeaderboard() != null) {
            showLeaderboard(n.getLeaderboard());
        }
        System.out.print("> ");
    }

    @Override
    public synchronized void onPlayerSummary(PlayerGameSummaryNotification n) {
        System.out.println("\n[NOTIFICATION] Personal summary for the concluded puzzle:");
        if (n.getPersonalInfo() != null) {
            showGameInfo(n.getPersonalInfo());
        }
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
