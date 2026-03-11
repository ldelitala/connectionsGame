package com.connections.client.ui;

import com.connections.common.dto.*;
import com.connections.common.protocol.server_responses.ServerResponse;

/**
 * Interface defining all possible output operations for the client.
 * This allows switching between CLI and GUI implementations.
 */
public interface ClientView {
    /**
     * Displays a general server response status and message.
     *
     * @param response The response received from the server.
     */
    void showResponse(ServerResponse<?> response);

    /**
     * Renders the current state of a puzzle board.
     *
     * @param info DTO containing found groups, remaining words, and mistakes.
     */
    void showGameInfo(PuzzleStateDTO info);

    /**
     * Displays global statistics for a specific puzzle.
     *
     * @param stats DTO containing player counts and average scores.
     */
    void showGameStats(PuzzleStatsDTO stats);

    /**
     * Displays the career statistics for the current player.
     *
     * @param stats DTO containing win rates and total scores.
     */
    void showPlayerStats(PlayerStatsDTO stats);

    /**
     * Displays a list of top players or a specific ranking.
     *
     * @param leaderboard DTO containing leaderboard entries.
     */
    void showLeaderboard(LeaderboardDTO leaderboard);

    /**
     * Displays an asynchronous notification (e.g., from UDP service).
     *
     * @param message The notification text.
     */
    void showNotification(String message);

    /**
     * Displays an error message to the user.
     *
     * @param error The error description.
     */
    void showError(String error);

    /**
     * Displays a general informational message.
     *
     * @param message The message text.
     */
    void showMessage(String message);

    /**
     * Renders the command-line prompt.
     *
     * @param prompt The prompt string (e.g., "> ").
     */
    void renderPrompt(String prompt);
}
