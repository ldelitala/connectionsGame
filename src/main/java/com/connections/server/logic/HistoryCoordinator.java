package com.connections.server.logic;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.client_requests.HistoryRequest;
import com.connections.common.protocol.server_responses.HistoryResponse;
import com.connections.common.protocol.server_responses.dto.*;
import com.connections.server.models.*;
import com.connections.server.persistence.HistoryDAO;

import java.util.*;

/**
 * Coordinator for historical puzzle data and global leaderboards.
 */
public class HistoryCoordinator {

    private final HistoryDAO historyDAO;
    private final UserCoordinator userCoordinator;
    private final List<LeaderboardEntryDTO> leaderboardCache = new ArrayList<>();

    public HistoryCoordinator(HistoryDAO historyDAO, UserCoordinator userCoordinator) {
        this.historyDAO = historyDAO;
        this.userCoordinator = userCoordinator;
        refreshLeaderboard();
    }

    /**
     * Centralized processing for history-related requests.
     */
    public HistoryResponse handle(HistoryRequest req, String username, PuzzleCoordinator puzzleCoordinator) {
        Operation op = req.getOperation();
        switch (op) {
            case GET_PAST_PUZZLE_INFO:
                PuzzleStateDTO state = puzzleCoordinator.getPersonalStatus(username, req.getPuzzleId());
                return (state != null) ? HistoryResponse.ok(op, state) : HistoryResponse.error(op, "History record not found.");

            case GET_PAST_PUZZLE_STATS:
                PuzzleStatsDTO stats = puzzleCoordinator.getGlobalStats(req.getPuzzleId());
                return (stats != null) ? HistoryResponse.ok(op, stats) : HistoryResponse.error(op, "Stats not available.");

            case GET_PLAYER_STATS:
                if (username == null) return HistoryResponse.forbidden(op, "Login required.");
                User u = userCoordinator.getUser(username);
                return HistoryResponse.ok(op, PlayerStatsDTO.create(
                        u.getPuzzlesCompleted(), u.getWinRate(), u.getLossRate(), u.getCurrentStreak(),
                        u.getMaxStreak(), u.getPerfectPuzzles(), u.getMistakeHistogram(), u.getTotalScore()));

            case GET_LEADERBOARD:
                if (req.getPlayerName() != null) {
                    LeaderboardEntryDTO rank = getPlayerRank(req.getPlayerName());
                    return (rank != null) ? HistoryResponse.ok(op, rank) : HistoryResponse.error(op, "Player not found.");
                }
                return HistoryResponse.ok(op, getGlobalLeaderboard(req.getTopPlayers()));

            default: return HistoryResponse.error(op, "Invalid history operation.");
        }
    }

    public synchronized void recordPuzzleResults(int puzzleId, Map<String, PlayerResult> results) {
        historyDAO.savePuzzleResults(puzzleId, results);
        refreshLeaderboard();
    }

    private synchronized void refreshLeaderboard() {
        // ... (sorting and mapping logic)
    }

    public LeaderboardDTO getGlobalLeaderboard(Integer topK) {
        int limit = (topK == null || topK <= 0) ? leaderboardCache.size() : Math.min(topK, leaderboardCache.size());
        return new LeaderboardDTO(new ArrayList<>(leaderboardCache.subList(0, limit)));
    }

    public LeaderboardEntryDTO getPlayerRank(String username) {
        for (LeaderboardEntryDTO entry : leaderboardCache) {
            if (entry.getUsername().equals(username)) return entry;
        }
        return null;
    }

    public PuzzleStateDTO getPastPuzzleState(String username, int puzzleId) {
        return null; 
    }

    public PuzzleStatsDTO getPastPuzzleStats(int puzzleId) {
        return null;
    }

    public void renameUser(String oldName, String newName) {
    }
}
