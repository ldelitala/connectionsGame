package it.unipi.connections.client.ui.console;

import it.unipi.connections.client.ui.ClientView;
import it.unipi.connections.common.protocol.server_responses.dto.*;
import it.unipi.connections.common.protocol.server_responses.ServerResponse;
import it.unipi.connections.common.notifications.*;

/**
 * CLI implementation of ClientView. Acts as a dispatcher that delegates actual painting to
 * ConsoleRenderer.
 */
public class ConsoleView implements ClientView {

    // formatter:off
    private static final String HELP_SYSTEM =
            "\n--- SYSTEM COMMANDS ---\n" +
                    "  help              : Show this list\n" +
                    "  exit              : Logout and close the application\n";

    private static final String HELP_LOGGEDIN =
            "\n--- AUTHENTICATION ---\n" +
                    "  register <uP> <psw>: Create a new account\n" +
                    "  login <uP> <psw>   : Sign in to your account\n";

    private static final String HELP_LOGGEDOUT =
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
                    "  update <old> <nU> <nP>: Update credentials (use '-' to skip username or password)\n"
                    +
                    "  logout            : Sign out of the session\n";
    // formatter:on

    @Override
    public synchronized void showResponse(ServerResponse<?> response) {
        String color = (response.getStatus() == it.unipi.connections.common.protocol.server_responses.Status.OK) ? "\u001B[32m" : "\u001B[31m";
        System.out.println(color + "[" + response.getStatus() + "] " + response.getMessage() + "\u001B[0m");
    }

    @Override
    public synchronized void render(DTO data) {
        if (data instanceof GuessResultDTO)
            ConsoleRenderer.paintGuessResult((GuessResultDTO) data);
        else if (data instanceof PuzzleStateDTO)
            ConsoleRenderer.paintPuzzleInfo((PuzzleStateDTO) data);
        else if (data instanceof PuzzleStatsDTO)
            ConsoleRenderer.paintPuzzleStats((PuzzleStatsDTO) data);
        else if (data instanceof PlayerStatsDTO)
            ConsoleRenderer.paintPlayerStats((PlayerStatsDTO) data);
        else if (data instanceof LeaderboardDTO)
            ConsoleRenderer.paintLeaderboard((LeaderboardDTO) data);
        else if (data instanceof LeaderboardEntryDTO)
            ConsoleRenderer.paintRank((LeaderboardEntryDTO) data);
    }

    @Override
    public synchronized void render(Notification n) {
        if (n instanceof WaitNotification) {
            ConsoleRenderer.paintWaitNotify((WaitNotification) n);
            return; // No extra prompt for countdown
        }

        if (n instanceof NewPuzzleNotification)
            ConsoleRenderer.paintNewPuzzleNotify((NewPuzzleNotification) n);
        else if (n instanceof PuzzleEndNotification)
            ConsoleRenderer.paintPuzzleEndNotify((PuzzleEndNotification) n);
        else if (n instanceof PlayerPuzzleSummaryNotification)
            ConsoleRenderer.paintPlayerSummaryNotify((PlayerPuzzleSummaryNotification) n);

        System.out.print("\n> "); // to ensure prompt is always on a new line after notifications
    }

    private static final String BANNER = 
        "\u001B[33m" +
        "  ____                            _   _                     \n" +
        " / ___|___  _ __  _ __   ___  ___| |_(_) ___  _ __  ___     \n" +
        "| |   / _ \\| '_ \\| '_ \\ / _ \\/ __| __| |/ _ \\| '_ \\/ __|    \n" +
        "| |__| (_) | | | | | | |  __/ (__| |_| | (_) | | | \\__ \\    \n" +
        " \\____\\___/|_| |_|_| |_|\\___|\\___|\\__|_|\\___/|_| |_|___/    \n" +
        "\u001B[0m" +
        "\u001B[36m" +
        "                ~ The Java NIO Game ~                       \n" +
        "\u001B[0m";

    @Override
    public synchronized void showHelp(boolean loggedIn) {
        if (!loggedIn) System.out.println(BANNER);
        System.out.print(HELP_SYSTEM);
        System.out.print(loggedIn ? HELP_LOGGEDOUT : HELP_LOGGEDIN);
        System.out.println();
    }

    @Override
    public synchronized void showError(String error) {
        System.err.println("\u001B[31m" + "Error: " + error + "\u001B[0m");
    }

    @Override
    public synchronized void showMessage(String message) {
        System.out.println("\u001B[34m" + message + "\u001B[0m");
    }

    @Override
    public synchronized void renderPrompt(String prompt) {
        System.out.print(prompt);
    }
}
