package com.connections.server.logic;

import com.connections.common.models.Category;
import com.connections.common.notifications.*;
import com.connections.common.protocol.server_responses.dto.*;
import com.connections.server.models.PlayerResult;
import com.connections.server.network.UdpNotifier;
import com.connections.server.models.Puzzle;
import com.connections.server.models.User;
import com.connections.server.persistence.PuzzlesDAO;
import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * The core game engine coordinator.
 * Manages active puzzle rounds, timer transitions, and player scoring.
 */
public class PuzzleCoordinator {

    private static final int POINTS_PER_GROUP = 6;
    private static final int PENALTY_PER_MISTAKE = 4;
    private static final int MAX_MISTAKES = 4;
    private static final int GROUPS_TO_WIN = 3;

    private final PuzzlesDAO puzzlesDAO;
    private final HistoryCoordinator historyCoordinator;
    private final UserCoordinator userCoordinator;
    private final UdpNotifier udpNotifier;
    private final long puzzleDurationMillis;
    private final Gson gson = new Gson();

    // Round state
    private Puzzle currentPuzzle;
    private long puzzleEndTime;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Player progress for the CURRENT round
    private final Map<String, List<Category>> playerProgress = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerMistakes = new ConcurrentHashMap<>();
    private final Set<String> completedPlayers = ConcurrentHashMap.newKeySet();

    public PuzzleCoordinator(PuzzlesDAO puzzlesDAO, HistoryCoordinator historyCoordinator, 
                             UserCoordinator userCoordinator, UdpNotifier udpNotifier, 
                             long puzzleDurationMillis) {
        this.puzzlesDAO = puzzlesDAO;
        this.historyCoordinator = historyCoordinator;
        this.userCoordinator = userCoordinator;
        this.udpNotifier = udpNotifier;
        this.puzzleDurationMillis = puzzleDurationMillis;
        
        startNewRound();
    }

    private synchronized void startNewRound() {
        // 1. Archive current if exists
        archiveRound();

        // 2. Load next
        currentPuzzle = puzzlesDAO.getNextPuzzle();
        if (currentPuzzle == null) {
            System.err.println("Fatal: No puzzles available in DAO!");
            return;
        }

        // 3. Reset state
        puzzleEndTime = System.currentTimeMillis() + puzzleDurationMillis;
        playerProgress.clear();
        playerMistakes.clear();
        completedPlayers.clear();

        // 4. Notify everyone
        System.out.println("New puzzle round started: ID " + currentPuzzle.getPuzzleId());
        if (udpNotifier != null) {
            NewPuzzleNotification n = NewPuzzleNotification.create(currentPuzzle.getPuzzleId(), currentPuzzle.getAllWords());
            udpNotifier.notifyAll(gson.toJson(n));
        }

        // 5. Schedule next transition
        scheduler.schedule(this::startNewRound, puzzleDurationMillis, TimeUnit.MILLISECONDS);
    }

    private synchronized void archiveRound() {
        if (currentPuzzle == null) return;

        int puzzleId = currentPuzzle.getPuzzleId();
        Map<String, PlayerResult> results = new HashMap<>();
        
        Set<String> participants = new HashSet<>(playerProgress.keySet());
        participants.addAll(playerMistakes.keySet());

        for (String username : participants) {
            PlayerResult res = createPlayerSnapshot(username);
            results.put(username, res);
            
            // Record final stats for players who haven't finished (DNF)
            if (!completedPlayers.contains(username)) {
                User u = userCoordinator.getUser(username);
                if (u != null) {
                    u.recordLoss(res.getMistakes(), res.getPoints());
                    userCoordinator.saveUser(u);
                }
            }
        }

        // Send to history coordinator for file persistence and leaderboard update
        historyCoordinator.archivePuzzle(puzzleId, results);

        // Notify concluding players
        if (udpNotifier != null) {
            PuzzleStatsDTO globalStats = calculateGlobalStats(puzzleId, results, false);
            LeaderboardDTO leaderboard = historyCoordinator.getGlobalLeaderboard(10);
            
            PuzzleEndNotification endN = PuzzleEndNotification.create(leaderboard, globalStats);
            udpNotifier.notifyAll(gson.toJson(endN));

            for (String username : participants) {
                PuzzleStateDTO personal = getPersonalStatus(username, puzzleId);
                if (personal != null) {
                    PlayerPuzzleSummaryNotification personalN = PlayerPuzzleSummaryNotification.create(personal);
                    udpNotifier.notifyUser(username, gson.toJson(personalN));
                }
            }
        }
    }

    public synchronized GuessResultDTO handleProposal(String username, List<String> words) {
        if (completedPlayers.contains(username) || getRemainingTime() <= 0) return null;
        if (words == null || words.size() != 4) return null;

        Category matched = currentPuzzle.getGroups().stream()
                .filter(g -> new HashSet<>(g.getWords()).equals(new HashSet<>(words)))
                .findFirst().orElse(null);

        if (matched != null) {
            List<Category> progress = playerProgress.computeIfAbsent(username, k -> new ArrayList<>());
            if (progress.contains(matched)) return null; 
            
            progress.add(matched);
            if (progress.size() == GROUPS_TO_WIN) finalizePlayer(username, true);
            
            PlayerResult snapshot = createPlayerSnapshot(username);
            return GuessResultDTO.success((float) POINTS_PER_GROUP, snapshot.getPoints(), getRemainingWords(username));
        } else {
            int m = playerMistakes.getOrDefault(username, 0) + 1;
            playerMistakes.put(username, m);
            if (m >= MAX_MISTAKES) finalizePlayer(username, false);
            
            PlayerResult snapshot = createPlayerSnapshot(username);
            return GuessResultDTO.wrong((float) -PENALTY_PER_MISTAKE, snapshot.getPoints(), getRemainingWords(username));
        }
    }

    private void finalizePlayer(String username, boolean won) {
        completedPlayers.add(username);
        PlayerResult res = createPlayerSnapshot(username);
        User u = userCoordinator.getUser(username);
        if (u != null) {
            if (won) u.recordWin(res.getMistakes(), res.getPoints());
            else u.recordLoss(res.getMistakes(), res.getPoints());
            userCoordinator.saveUser(u);
        }
    }

    public PuzzleStateDTO getPersonalStatus(String username, int puzzleId) {
        // Check current round
        if (currentPuzzle != null && puzzleId == currentPuzzle.getPuzzleId()) {
            PlayerResult res = createPlayerSnapshot(username);
            return PuzzleStateDTO.active(puzzleId, getRemainingTime(), 
                    playerProgress.getOrDefault(username, Collections.emptyList()), 
                    getRemainingWords(username), res.getMistakes(), res.getPoints());
        }
        
        // Check history
        Map<String, PlayerResult> past = historyCoordinator.getPastResults(puzzleId);
        if (past != null && past.containsKey(username)) {
            PlayerResult res = past.get(username);
            Puzzle old = puzzlesDAO.getPuzzle(puzzleId);
            return PuzzleStateDTO.concluded(puzzleId, (old != null) ? old.getGroups() : Collections.emptyList(), 
                    res.getCorrectGroups(), res.getMistakes(), res.getPoints());
        }
        return null;
    }

    public PuzzleStatsDTO getGlobalStats(int puzzleId) {
        if (currentPuzzle != null && puzzleId == currentPuzzle.getPuzzleId()) {
            Map<String, PlayerResult> currentResults = new HashMap<>();
            Set<String> participants = new HashSet<>(playerProgress.keySet());
            participants.addAll(playerMistakes.keySet());
            for (String u : participants) currentResults.put(u, createPlayerSnapshot(u));
            return calculateGlobalStats(puzzleId, currentResults, true);
        }
        
        Map<String, PlayerResult> past = historyCoordinator.getPastResults(puzzleId);
        return (past != null) ? calculateGlobalStats(puzzleId, past, false) : null;
    }

    private PuzzleStatsDTO calculateGlobalStats(int puzzleId, Map<String, PlayerResult> results, boolean active) {
        int winners = 0, finished = 0, totalScore = 0;
        for (PlayerResult r : results.values()) {
            if (r.isWon()) winners++;
            if (r.isFinished()) finished++;
            totalScore += r.getPoints();
        }
        if (active) return PuzzleStatsDTO.active(puzzleId, getRemainingTime(), results.size() - finished, finished, winners);
        else return PuzzleStatsDTO.concluded(puzzleId, results.size(), finished, winners, 
                                            results.isEmpty() ? 0.0 : (double) totalScore / results.size());
    }

    private PlayerResult createPlayerSnapshot(String username) {
        int m = playerMistakes.getOrDefault(username, 0);
        int c = playerProgress.getOrDefault(username, Collections.emptyList()).size();
        return new PlayerResult(m, c, completedPlayers.contains(username));
    }

    private List<String> getRemainingWords(String username) {
        List<Category> found = playerProgress.getOrDefault(username, Collections.emptyList());
        Set<String> foundWords = found.stream().flatMap(g -> g.getWords().stream()).collect(Collectors.toSet());
        return currentPuzzle.getAllWords().stream().filter(w -> !foundWords.contains(w)).collect(Collectors.toList());
    }

    public long getRemainingTime() {
        return Math.max(0, puzzleEndTime - System.currentTimeMillis());
    }
    
    public Puzzle getCurrentPuzzle() { return currentPuzzle; }
}
