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

    public ClientRequest preparePastInfo(String[] tokens, ClientView view) {
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

    public ClientRequest prepareGlobalStats(String[] tokens, ClientView view) {
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

    public ClientRequest prepareRanking(String[] tokens, ClientView view) {
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

    public ClientRequest prepareHistory() {
        return HistoryRequest.getPlayerStats();
    }

    public void handleResponse(ServerResponse<?> response, ClientView view) {
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
