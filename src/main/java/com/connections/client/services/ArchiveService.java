package com.connections.client.services;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;

/**
 * Handles operations and display for past puzzles and global stats.
 */
public class ArchiveService {

    private static final int MIN_TOKENS_ARG = 2;
    private static final int INDEX_ARG_1 = 1;
    private static final int INDEX_ARG_2 = 2;
    private static final String FLAG_TOP = "-top";

    private final ClientView view;

    /**
     * Initializes the archive service.
     *
     * @param view The UI view to display historical data and rankings.
     */
    public ArchiveService(ClientView view) {
        this.view = view;
    }

    /**
     * Prepares a request for detailed information about a past puzzle.
     *
     * @param tokens The command tokens (expected: "info", puzzleId).
     * @return A {@link ClientRequest} for puzzle info, or null if ID is invalid.
     */
    public ClientRequest preparePastInfo(String[] tokens) {
        if (tokens.length < MIN_TOKENS_ARG) {
            view.showMessage("Usage: info [id]");
            return null;
        }
        try {
            return HistoryRequest.getGameInfo(Integer.parseInt(tokens[INDEX_ARG_1]));
        } catch (NumberFormatException e) {
            view.showError("Invalid puzzle ID.");
            return null;
        }
    }

    /**
     * Prepares a request for global statistics of a past puzzle.
     *
     * @param tokens The command tokens (expected: "global", puzzleId).
     * @return A {@link ClientRequest} for puzzle stats, or null if ID is invalid.
     */
    public ClientRequest prepareGlobalStats(String[] tokens) {
        if (tokens.length < MIN_TOKENS_ARG) {
            view.showMessage("Usage: global [id]");
            return null;
        }
        try {
            return HistoryRequest.getGameStats(Integer.parseInt(tokens[INDEX_ARG_1]));
        } catch (NumberFormatException e) {
            view.showError("Invalid puzzle ID.");
            return null;
        }
    }

    /**
     * Prepares a leaderboard ranking request.
     * Supports fetching a specific player's rank or the top K players.
     *
     * @param tokens The command tokens.
     * @return A {@link ClientRequest} for the leaderboard, or null if usage is incorrect.
     */
    public ClientRequest prepareRanking(String[] tokens) {
        if (tokens.length < MIN_TOKENS_ARG) {
            view.showMessage("Usage:\n" +
                             "  ranking <username>      : View a specific player's rank\n" +
                             "  ranking -top [k]        : View top k players");
            return null;
        }

        String firstArg = tokens[INDEX_ARG_1];
        if (firstArg.equalsIgnoreCase(FLAG_TOP)) {
            Integer k = (tokens.length > INDEX_ARG_2) ? Integer.parseInt(tokens[INDEX_ARG_2]) : null;
            return HistoryRequest.getLeaderboard(null, k);
        } else {
            return HistoryRequest.getLeaderboard(firstArg, null);
        }
    }

    /**
     * Prepares a request for the current user's historical career statistics.
     *
     * @return A {@link ClientRequest} for player stats.
     */
    public ClientRequest prepareHistory() {
        return HistoryRequest.getPlayerStats();
    }

    /**
     * Processes a server response related to historical data and updates the view.
     *
     * @param response The server response to handle.
     */
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
