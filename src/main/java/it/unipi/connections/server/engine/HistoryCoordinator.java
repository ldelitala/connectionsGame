package it.unipi.connections.server.engine;

import it.unipi.connections.common.models.Category;
import it.unipi.connections.common.protocol.Operation;
import it.unipi.connections.common.protocol.client_requests.HistoryRequest;
import it.unipi.connections.common.protocol.server_responses.ServerResponse;
import it.unipi.connections.common.protocol.server_responses.dto.*;
import it.unipi.connections.server.models.*;
import it.unipi.connections.server.persistence.HistoryDAO;
import it.unipi.connections.server.persistence.PuzzlesDAO;
import it.unipi.connections.server.persistence.UsersDAO;

import java.util.*;

/**
 * Coordinator for historical puzzle data and global leaderboards.
 * Responsible for operations under the "HISTORY & STATS" section of the protocol.
 */
public class HistoryCoordinator {

    private final HistoryDAO historyDAO;
    private final UsersDAO usersDAO;
    private final PuzzlesDAO puzzlesDAO;
    private final List<LeaderboardEntryDTO> leaderboardCache = new ArrayList<>();

    public HistoryCoordinator(HistoryDAO historyDAO, UsersDAO usersDAO, PuzzlesDAO puzzlesDAO) {
        this.historyDAO = historyDAO;
        this.usersDAO = usersDAO;
        this.puzzlesDAO = puzzlesDAO;
        refreshLeaderboard();
    }

    /**
     * Handles all history and statistics related requests defined in Operation.java.
     */
    public ServerResponse<?> handle(HistoryRequest req, Operation op, String username) {
        switch (op) {
            case GET_PAST_PUZZLE_INFO:
                if (req.getPuzzleId() == null) {
                    return ServerResponse.error(op, "Puzzle ID is required for historical info.");
                }
                PuzzleStateDTO pastInfo = getPastPuzzleState(username, req.getPuzzleId());
                return (pastInfo != null) ? ServerResponse.ok(op, pastInfo) 
                                          : ServerResponse.error(op, "Historical puzzle info not found.");

            case GET_PAST_PUZZLE_STATS:
                if (req.getPuzzleId() == null) {
                    return ServerResponse.error(op, "Puzzle ID is required for historical stats.");
                }
                PuzzleStatsDTO pastStats = getPastPuzzleStats(req.getPuzzleId());
                return (pastStats != null) ? ServerResponse.ok(op, pastStats) 
                                           : ServerResponse.error(op, "Historical puzzle stats not available.");

            case GET_PLAYER_STATS:
                if (username == null) return ServerResponse.forbidden(op, "Login required.");
                User u = usersDAO.getUser(username);
                if (u == null) return ServerResponse.error(op, "User data not found.");
                
                return ServerResponse.ok(op, PlayerStatsDTO.create(
                        u.getPuzzlesCompleted(), u.getWinRate(), u.getLossRate(), u.getCurrentStreak(),
                        u.getMaxStreak(), u.getPerfectPuzzles(), u.getMistakeHistogram(), u.getTotalScore()));

            case GET_LEADERBOARD:
                if (req.getPlayerName() != null) {
                    LeaderboardEntryDTO rank = getPlayerRank(req.getPlayerName());
                    return (rank != null) ? ServerResponse.ok(op, LeaderboardDTO.create(Collections.singletonList(rank))) 
                                         : ServerResponse.error(op, "Player not found in leaderboard.");
                }
                return ServerResponse.ok(op, getGlobalLeaderboard(req.getTopPlayers()));

            default: 
                return ServerResponse.error(op, "Unsupported history operation: " + op);
        }
    }

    public synchronized void recordPuzzleResults(int puzzleId, Map<String, PlayerResult> results) {
        historyDAO.savePuzzleResults(puzzleId, results);
        refreshLeaderboard();
    }

    private synchronized void refreshLeaderboard() {
        List<User> users = usersDAO.getAllUsers();
        // Sort by totalScore descending
        users.sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()));
        
        leaderboardCache.clear();
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            leaderboardCache.add(LeaderboardEntryDTO.create(i + 1, u.getUsername(), u.getTotalScore()));
        }
    }

    public LeaderboardDTO getGlobalLeaderboard(Integer topK) {
        int limit = (topK == null || topK <= 0) ? leaderboardCache.size() : Math.min(topK, leaderboardCache.size());
        return LeaderboardDTO.create(new ArrayList<>(leaderboardCache.subList(0, limit)));
    }

    public LeaderboardEntryDTO getPlayerRank(String username) {
        for (LeaderboardEntryDTO entry : leaderboardCache) {
            if (entry.getUsername().equals(username)) return entry;
        }
        return null;
    }

    public PuzzleStateDTO getPastPuzzleState(String username, int puzzleId) {
        Map<String, PlayerResult> results = historyDAO.getPuzzleResults(puzzleId);
        if (results == null) return null;

        PlayerResult playerRes = results.get(username);
        Puzzle pDef = puzzlesDAO.getPuzzle(puzzleId);
        
        return PuzzleStateDTO.concluded(
            puzzleId,
            pDef != null ? pDef.getGroups() : null,
            playerRes != null ? playerRes.getCorrectGroups() : 0,
            playerRes != null ? playerRes.getMistakes() : 0,
            playerRes != null ? playerRes.getPoints() : 0
        );
    }

    public PuzzleStatsDTO getPastPuzzleStats(int puzzleId) {
        Map<String, PlayerResult> results = historyDAO.getPuzzleResults(puzzleId);
        if (results == null) return null;

        int total = results.size();
        int winners = (int) results.values().stream().filter(PlayerResult::isWon).count();
        double avgScore = results.values().stream().mapToInt(PlayerResult::getPoints).average().orElse(0.0);

        return PuzzleStatsDTO.concluded(puzzleId, total, total, winners, avgScore);
    }

    public void renameUser(String oldName, String newName) {
        // Logic to update cache keys if necessary
    }
}
