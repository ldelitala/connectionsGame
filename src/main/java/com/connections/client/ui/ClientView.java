package com.connections.client.ui;

import com.connections.common.dto.*;
import com.connections.common.protocol.server_responses.ServerResponse;

/**
 * Interface defining all possible output operations for the client.
 * This allows switching between CLI and GUI implementations.
 */
public interface ClientView {
    void showResponse(ServerResponse<?> response);
    void showGameInfo(PuzzleStateDTO info);
    void showGameStats(PuzzleStatsDTO stats);
    void showPlayerStats(PlayerStatsDTO stats);
    void showLeaderboard(LeaderboardDTO leaderboard);
    void showNotification(String message);
    void showError(String error);
    void showMessage(String message);
    void renderPrompt(String prompt);
}
