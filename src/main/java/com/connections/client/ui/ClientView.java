package com.connections.client.ui;

import com.connections.common.protocol.server_responses.dto.*;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.notifications.*;

/**
 * Interface defining all possible output operations for the client.
 * Specialized for different puzzle events and notifications.
 */
public interface ClientView {
    void showResponse(ServerResponse<?> response);
    void showGuessResult(GuessResultDTO result);
    void showPuzzleInfo(PuzzleStateDTO info);
    void showPuzzleStats(PuzzleStatsDTO stats);
    void showPlayerStats(PlayerStatsDTO stats);
    void showLeaderboard(LeaderboardDTO leaderboard);

    /** Displays the help menu based on the current authentication state. */
    void showHelp(boolean loggedIn);
    
    // Specific notification callbacks
    void onNewPuzzle(NewPuzzleNotification notification);
    void onPuzzleEnd(PuzzleEndNotification notification);
    void onPlayerSummary(PlayerPuzzleSummaryNotification notification);
    
    void showError(String error);
    void showMessage(String message);
    void renderPrompt(String prompt);
}
