package com.connections.server.logic;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.client_requests.GameplayRequest;
import com.connections.common.protocol.server_responses.GameplayResponse;
import com.connections.common.protocol.server_responses.dto.*;
import com.connections.server.network.UdpNotifier;
import com.connections.server.models.*;
import com.connections.server.persistence.PuzzlesDAO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the lifecycle of active puzzles and user interactions during a round.
 */
public class PuzzleCoordinator {

    private final PuzzlesDAO puzzlesDAO;
    private final UserCoordinator userCoordinator;
    private final HistoryCoordinator historyCoordinator;
    private final UdpNotifier udpNotifier;

    private Puzzle currentPuzzle;
    
    // Volatile state for the current puzzle
    private final Map<String, Integer> playerMistakes = new ConcurrentHashMap<>();
    private final Set<String> completedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, List<List<String>>> playerCorrectGuesses = new ConcurrentHashMap<>();

    public PuzzleCoordinator(PuzzlesDAO puzzlesDAO, UserCoordinator userCoordinator, 
                             HistoryCoordinator historyCoordinator, UdpNotifier udpNotifier) {
        this.puzzlesDAO = puzzlesDAO;
        this.userCoordinator = userCoordinator;
        this.historyCoordinator = historyCoordinator;
        this.udpNotifier = udpNotifier;
        loadNextPuzzle();
    }

    /**
     * Centralized processing for gameplay-related requests.
     */
    public GameplayResponse handle(GameplayRequest req, Operation op, String username) {
        if (username == null) return GameplayResponse.forbidden(op, "Login required.");
        
        switch (op) {
            case SUBMIT_PROPOSAL:
                GuessResultDTO result = handleProposal(username, req.getWords());
                return (result != null) ? GameplayResponse.ok(op, "Guess processed.", result) 
                                        : GameplayResponse.error(op, "Invalid or late guess.");

            case GET_PUZZLE_INFO:
                PuzzleStateDTO state = getPersonalStatus(username, currentPuzzle.getPuzzleId());
                return (state != null) ? GameplayResponse.ok(op, state) : GameplayResponse.error(op, "Puzzle not found.");

            case GET_PUZZLE_STATS:
                PuzzleStatsDTO stats = getGlobalStats(currentPuzzle.getPuzzleId());
                return (stats != null) ? GameplayResponse.ok(op, stats) : GameplayResponse.error(op, "Stats not available.");

            default: return GameplayResponse.error(op, "Invalid gameplay operation.");
        }
    }

    private synchronized void loadNextPuzzle() {
        this.currentPuzzle = puzzlesDAO.getRandomPuzzle();
        playerMistakes.clear();
        completedPlayers.clear();
        playerCorrectGuesses.clear();
        
        if (currentPuzzle != null) {
            udpNotifier.broadcastNewPuzzle(currentPuzzle.getPuzzleId(), currentPuzzle.getAllWords());
        }
    }

    public synchronized GuessResultDTO handleProposal(String username, List<String> words) {
        if (currentPuzzle == null || completedPlayers.contains(username)) return null;

        int mistakes = playerMistakes.getOrDefault(username, 0);
        if (mistakes >= 4) return null;

        boolean isCorrect = currentPuzzle.isCorrectGroup(words);
        double pointsDelta;
        String status;

        if (isCorrect) {
            List<List<String>> correctOnes = playerCorrectGuesses.computeIfAbsent(username, k -> new ArrayList<>());
            // Check if already found
            for (List<String> found : correctOnes) {
                if (new HashSet<>(found).equals(new HashSet<>(words))) return null; 
            }
            correctOnes.add(words);
            pointsDelta = currentPuzzle.getPointsForWords(words);
            status = "success";
            
            if (correctOnes.size() == 4) {
                completedPlayers.add(username);
                recordEndGameStats(username, true);
            }
        } else {
            mistakes++;
            playerMistakes.put(username, mistakes);
            pointsDelta = -1.0;
            status = "wrong";
            
            if (mistakes >= 4) {
                completedPlayers.add(username);
                recordEndGameStats(username, false);
            }
        }

        User u = userCoordinator.getUser(username);
        return GuessResultDTO.create(status, pointsDelta, u.getTotalScore(), getRemainingWords(username));
    }

    private void recordEndGameStats(String username, boolean won) {
        PlayerResult res = createPlayerSnapshot(username);
        User u = userCoordinator.getUser(username);
        if (u != null) {
            if (won) u.recordWin(res.getMistakes(), res.getPoints());
            else u.recordLoss(res.getMistakes(), res.getPoints());
            userCoordinator.saveUser(u);
        }
    }

    public void finalizePuzzle() {
        Map<String, PlayerResult> results = new HashMap<>();
        Set<String> participants = new HashSet<>(playerMistakes.keySet());
        participants.addAll(playerCorrectGuesses.keySet());
        participants.addAll(completedPlayers);

        for (String username : participants) {
            PlayerResult res = createPlayerSnapshot(username);
            results.put(username, res);
            
            if (!completedPlayers.contains(username)) {
                User u = userCoordinator.getUser(username);
                if (u != null) {
                    u.recordLoss(res.getMistakes(), res.getPoints());
                    userCoordinator.saveUser(u);
                }
            }
        }

        historyCoordinator.recordPuzzleResults(currentPuzzle.getPuzzleId(), results);
        udpNotifier.broadcastPuzzleEnd(currentPuzzle.getPuzzleId(), 
                historyCoordinator.getGlobalLeaderboard(10), getGlobalStats(currentPuzzle.getPuzzleId()));
        
        for (String username : participants) {
            udpNotifier.sendPlayerSummary(username, getPersonalStatus(username, currentPuzzle.getPuzzleId()));
        }

        loadNextPuzzle();
    }

    public PuzzleStateDTO getPersonalStatus(String username, int puzzleId) {
        if (currentPuzzle != null && currentPuzzle.getPuzzleId() == puzzleId) {
            return PuzzleStateDTO.create(
                puzzleId, 
                completedPlayers.contains(username),
                30000, // Dummy time
                currentPuzzle.getCategoriesForGroups(playerCorrectGuesses.getOrDefault(username, Collections.emptyList())),
                getRemainingWords(username),
                playerMistakes.getOrDefault(username, 0),
                userCoordinator.getUser(username).getTotalScore()
            );
        }
        return historyCoordinator.getPastPuzzleState(username, puzzleId);
    }

    public PuzzleStatsDTO getGlobalStats(int puzzleId) {
        if (currentPuzzle != null && currentPuzzle.getPuzzleId() == puzzleId) {
            int active = playerMistakes.size() - completedPlayers.size();
            long won = completedPlayers.stream()
                .filter(u -> playerCorrectGuesses.getOrDefault(u, Collections.emptyList()).size() == 4)
                .count();
            return PuzzleStatsDTO.active(puzzleId, active, completedPlayers.size(), (int)won);
        }
        return historyCoordinator.getPastPuzzleStats(puzzleId);
    }

    private List<String> getRemainingWords(String username) {
        List<String> all = currentPuzzle.getAllWords();
        List<List<String>> found = playerCorrectGuesses.getOrDefault(username, Collections.emptyList());
        List<String> remaining = new ArrayList<>(all);
        for (List<String> group : found) {
            remaining.removeAll(group);
        }
        return remaining;
    }

    private PlayerResult createPlayerSnapshot(String username) {
        return new PlayerResult(
            username,
            playerCorrectGuesses.getOrDefault(username, Collections.emptyList()).size() == 4,
            playerMistakes.getOrDefault(username, 0),
            playerCorrectGuesses.getOrDefault(username, Collections.emptyList()).size(),
            0.0 // Points logic can be refined
        );
    }

    public Puzzle getCurrentPuzzle() {
        return currentPuzzle;
    }
}
