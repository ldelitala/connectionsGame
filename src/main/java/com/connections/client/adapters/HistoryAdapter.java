package com.connections.client.adapters;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;

/**
 * Handles operations related to past puzzles, statistics, and leaderboards.
 */
public class HistoryAdapter {

    private final ClientView view;

    public HistoryAdapter(ClientView view) {
        this.view = view;
    }

    /** Prepares a request for info about a past puzzle. */
    public ClientRequest preparePastInfo(int puzzleId) {
        return HistoryRequest.getGameInfo(puzzleId);
    }

    /** Prepares a request for global stats of a past puzzle. */
    public ClientRequest prepareGlobalStats(int puzzleId) {
        return HistoryRequest.getGameStats(puzzleId);
    }

    /** Prepares a request for the leaderboard or player rank. */
    public ClientRequest prepareRanking(String username, Integer topK) {
        return HistoryRequest.getLeaderboard(username, topK);
    }

    /** Prepares a request for the current user's career history. */
    public ClientRequest prepareHistory() {
        return HistoryRequest.getPlayerStats();
    }

    /** Interprets and displays archive-related responses. */
    public void handleResponse(ServerResponse<?> response) {
        view.showResponse(response);
        Object data = response.getData();
        if (data instanceof PuzzleStateDTO) {
            view.showGameInfo((PuzzleStateDTO) data);
        } else if (data instanceof PuzzleStatsDTO) {
            view.showGameStats((PuzzleStatsDTO) data);
        } else if (data instanceof PlayerStatsDTO) {
            view.showPlayerStats((PlayerStatsDTO) data);
        } else if (data instanceof LeaderboardDTO) {
            view.showLeaderboard((LeaderboardDTO) data);
        }
    }
}
