package com.connections.server.logic;

import com.connections.common.protocol.server_responses.dto.LeaderboardDTO;
import com.connections.common.protocol.server_responses.dto.LeaderboardEntryDTO;
import com.connections.server.models.PlayerResult;
import com.connections.server.models.User;
import com.connections.server.persistence.HistoryDAO;
import com.connections.server.persistence.UsersDAO;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Coordinator for history and ranking logic.
 * Maintains an in-memory Top K leaderboard for fast access.
 */
public class HistoryCoordinator {
    private static final int TOP_K_LIMIT = 100; // Max size for RAM leaderboard

    private final HistoryDAO historyDAO;
    private final UsersDAO usersDAO;
    
    // In-memory leaderboard for fast access
    private final List<LeaderboardEntryDTO> leaderboardCache = new CopyOnWriteArrayList<>();

    public HistoryCoordinator(HistoryDAO historyDAO, UsersDAO usersDAO) {
        this.historyDAO = historyDAO;
        this.usersDAO = usersDAO;
        refreshLeaderboard();
    }

    /**
     * Refreshes the Top K leaderboard from the UsersDAO.
     */
    public synchronized void refreshLeaderboard() {
        List<User> allUsers = usersDAO.getAllUsers();
        List<LeaderboardEntryDTO> topUsers = allUsers.stream()
                .sorted(Comparator.comparingInt(User::getTotalScore).reversed())
                .limit(TOP_K_LIMIT)
                .map(u -> LeaderboardEntryDTO.create(0, u.getUsername(), u.getTotalScore()))
                .collect(Collectors.toList());
        
        // Assign ranks
        for (int i = 0; i < topUsers.size(); i++) {
            LeaderboardEntryDTO entry = topUsers.get(i);
            leaderboardCache.add(LeaderboardEntryDTO.create(i + 1, entry.getUsername(), entry.getScore()));
        }
        
        // Swap cache (simplified since it's small)
        leaderboardCache.clear();
        for (int i = 0; i < topUsers.size(); i++) {
            LeaderboardEntryDTO e = topUsers.get(i);
            leaderboardCache.add(LeaderboardEntryDTO.create(i + 1, e.getUsername(), e.getScore()));
        }
    }

    public LeaderboardDTO getGlobalLeaderboard(Integer limit) {
        int k = (limit == null) ? leaderboardCache.size() : Math.min(limit, leaderboardCache.size());
        return LeaderboardDTO.create(new ArrayList<>(leaderboardCache.subList(0, k)));
    }

    public LeaderboardEntryDTO getPlayerRank(String username) {
        // Try cache first
        for (LeaderboardEntryDTO entry : leaderboardCache) {
            if (entry.getUsername().equals(username)) return entry;
        }
        
        // Not in Top K, calculate rank from all users (slow path)
        List<User> all = usersDAO.getAllUsers();
        all.sort(Comparator.comparingInt(User::getTotalScore).reversed());
        
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getUsername().equals(username)) {
                return LeaderboardEntryDTO.create(i + 1, username, all.get(i).getTotalScore());
            }
        }
        return null;
    }

    public void archivePuzzle(int puzzleId, Map<String, PlayerResult> results) {
        historyDAO.archivePuzzle(puzzleId, results);
        refreshLeaderboard(); // Update rankings after round ends
    }

    public Map<String, PlayerResult> getPastResults(int puzzleId) {
        return historyDAO.getPuzzleResults(puzzleId);
    }
}
