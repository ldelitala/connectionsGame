package com.connections.server;

import com.connections.server.models.Puzzle;
import com.connections.server.models.User;
import com.connections.common.models.Category;
import com.connections.common.notifications.*;
import com.connections.common.protocol.server_responses.dto.*;
import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages the active puzzle session and player progress.
 * Orchestrates the transition between puzzle sessions and historical archiving.
 */
public class PuzzleSessionManager {

    // --- Scoring Rules ---
    private static final int POINTS_PER_GROUP = 6;
    private static final int PENALTY_PER_MISTAKE = 4;
    private static final int MAX_MISTAKES = 4;
    private static final int GROUPS_TO_WIN = 3; // 4th is implicit

    // --- Configuration ---
    private static final int TOP_K_LEADERBOARD = 10;
    private static final int WORDS_PER_PROPOSAL = 4;

    private final PuzzlesDAO puzzlesDAO;
    private final UserManager userManager;
    private final UdpNotifier udpNotifier;
    private final long puzzleDurationMillis;
    private Puzzle currentPuzzle;
    private long puzzleEndTime;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, List<Category>> playerProgress = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerMistakes = new ConcurrentHashMap<>();
    private final Set<String> completedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Map<String, PlayerResult>> puzzleHistory = new ConcurrentHashMap<>();

    public PuzzleSessionManager(PuzzlesDAO puzzlesDAO, UserManager userManager, UdpNotifier udpNotifier, long puzzleDurationMillis) {
        this.puzzlesDAO = puzzlesDAO;
        this.userManager = userManager;
        this.udpNotifier = udpNotifier;
        this.puzzleDurationMillis = puzzleDurationMillis;
        startNewPuzzle();
    }

    private PlayerResult getPlayerSnapshot(String username) {
        int mistakes = playerMistakes.getOrDefault(username, 0);
        int correctGroups = playerProgress.getOrDefault(username, Collections.emptyList()).size();
        boolean finished = completedPlayers.contains(username);
        return new PlayerResult(mistakes, correctGroups, finished);
    }

    private void archiveCurrentPuzzle() {
        if (currentPuzzle == null) return;
        int puzzleId = currentPuzzle.getPuzzleId();
        Map<String, PlayerResult> results = new HashMap<>();
        
        Set<String> participants = new HashSet<>(playerProgress.keySet());
        participants.addAll(playerMistakes.keySet());
        
        for (String user : participants) {
            results.put(user, getPlayerSnapshot(user));
        }
        puzzleHistory.put(puzzleId, results);

        if (udpNotifier != null) {
            Gson gson = new Gson();
            PuzzleStatsDTO stats = getGlobalPuzzleStats(puzzleId);
            List<User> topPlayers = userManager.getTopK(TOP_K_LEADERBOARD);
            List<LeaderboardEntryDTO> leaderboardEntries = new ArrayList<>();
            for (int i = 0; i < topPlayers.size(); i++) {
                User u = topPlayers.get(i);
                leaderboardEntries.add(LeaderboardEntryDTO.create(i + 1, u.getUsername(), u.getTotalScore()));
            }
            LeaderboardDTO leaderboard = LeaderboardDTO.create(leaderboardEntries);
            
            String globalMsg = gson.toJson(PuzzleEndNotification.create(leaderboard, stats));
            udpNotifier.notifyAll(globalMsg);

            for (String user : participants) {
                PuzzleStateDTO personal = getPersonalPuzzleStatus(user, puzzleId);
                if (personal != null) {
                    String personalMsg = gson.toJson(PlayerPuzzleSummaryNotification.create(personal));
                    udpNotifier.notifyUser(user, personalMsg);
                }
            }
        }
    }

    private void startNewPuzzle() {
        archiveCurrentPuzzle();
        currentPuzzle = puzzlesDAO.getNextPuzzle();
        if (currentPuzzle == null) {
            System.err.println("No puzzles available!");
            return;
        }
        puzzleEndTime = System.currentTimeMillis() + puzzleDurationMillis;
        playerProgress.clear();
        playerMistakes.clear();
        completedPlayers.clear();
        
        System.out.println(String.format("New puzzle started: ID %d", currentPuzzle.getPuzzleId()));
        if (udpNotifier != null) {
            NewPuzzleNotification notification = NewPuzzleNotification.create(currentPuzzle.getPuzzleId(), currentPuzzle.getAllWords());
            udpNotifier.notifyAll(new Gson().toJson(notification));
        }
        scheduler.schedule(this::startNewPuzzle, puzzleDurationMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized GuessResultDTO submitProposal(String username, List<String> words) {
        if (completedPlayers.contains(username)) return null;
        if (getRemainingTime() <= 0) return null;
        if (words == null || words.size() != WORDS_PER_PROPOSAL) return null;
        
        Category matched = currentPuzzle.getGroups().stream()
                .filter(g -> new HashSet<>(g.getWords()).equals(new HashSet<>(words)))
                .findFirst().orElse(null);

        if (matched != null) {
            List<Category> progress = playerProgress.computeIfAbsent(username, k -> new ArrayList<>());
            if (progress.contains(matched)) return null; 
            
            progress.add(matched);
            boolean win = (progress.size() == GROUPS_TO_WIN); 
            if (win) finalizePlayerPuzzle(username, true);
            
            PlayerResult snapshot = getPlayerSnapshot(username);
            return GuessResultDTO.success((float) POINTS_PER_GROUP, snapshot.getPoints(), getRemainingWords(username));
        } else {
            int m = playerMistakes.getOrDefault(username, 0) + 1;
            playerMistakes.put(username, m);
            boolean lose = (m >= MAX_MISTAKES);
            if (lose) finalizePlayerPuzzle(username, false);
            
            PlayerResult snapshot = getPlayerSnapshot(username);
            return GuessResultDTO.wrong((float) -PENALTY_PER_MISTAKE, snapshot.getPoints(), getRemainingWords(username));
        }
    }

    private List<String> getRemainingWords(String username) {
        List<Category> foundGroups = playerProgress.getOrDefault(username, Collections.emptyList());
        List<String> foundWords = foundGroups.stream()
                .flatMap(g -> g.getWords().stream())
                .collect(Collectors.toList());
        return currentPuzzle.getAllWords().stream()
                .filter(w -> !foundWords.contains(w))
                .collect(Collectors.toList());
    }

    private void finalizePlayerPuzzle(String username, boolean won) {
        completedPlayers.add(username);
        PlayerResult res = getPlayerSnapshot(username);
        User u = userManager.getUser(username);
        if (won) u.recordWin(res.getMistakes(), res.getPoints());
        else u.recordLoss(res.getMistakes(), res.getPoints());
    }

    public long getRemainingTime() {
        return Math.max(0, puzzleEndTime - System.currentTimeMillis());
    }

    public Puzzle getCurrentPuzzle() { return currentPuzzle; }
    public List<Category> getPlayerProgress(String username) { return playerProgress.getOrDefault(username, Collections.emptyList()); }
    public int getPlayerMistakes(String username) { return playerMistakes.getOrDefault(username, 0); }

    public synchronized PuzzleStateDTO getPersonalPuzzleStatus(String username, int puzzleId) {
        if (currentPuzzle != null && puzzleId == currentPuzzle.getPuzzleId()) {
            PlayerResult res = getPlayerSnapshot(username);
            List<Category> foundGroups = playerProgress.getOrDefault(username, Collections.emptyList());
            List<String> foundWords = foundGroups.stream()
                    .flatMap(g -> g.getWords().stream())
                    .collect(Collectors.toList());
            List<String> remainingWords = currentPuzzle.getAllWords().stream()
                    .filter(w -> !foundWords.contains(w))
                    .collect(Collectors.toList());
            
            return PuzzleStateDTO.active(puzzleId, getRemainingTime(), foundGroups, remainingWords, res.getMistakes(), res.getPoints());
        } 
        
        Map<String, PlayerResult> history = puzzleHistory.get(puzzleId);
        if (history != null && history.containsKey(username)) {
            PlayerResult res = history.get(username);
            Puzzle oldPuzzle = puzzlesDAO.getPuzzle(puzzleId);
            List<Category> allGroups = (oldPuzzle != null) ? oldPuzzle.getGroups() : Collections.emptyList();
            
            return PuzzleStateDTO.concluded(puzzleId, allGroups, res.getCorrectGroups(), res.getMistakes(), res.getPoints());
        }
        return null;
    }

    public synchronized PuzzleStatsDTO getGlobalPuzzleStats(int puzzleId) {
        Map<String, PlayerResult> results;
        boolean isActive = false;
        
        if (currentPuzzle != null && puzzleId == currentPuzzle.getPuzzleId()) {
            isActive = true;
            results = new HashMap<>();
            Set<String> participants = new HashSet<>(playerProgress.keySet());
            participants.addAll(playerMistakes.keySet());
            for (String user : participants) {
                results.put(user, getPlayerSnapshot(user));
            }
        } else {
            results = puzzleHistory.get(puzzleId);
        }
        
        if (results == null) return null;
        
        int winners = 0;
        int finishedCount = 0;
        int totalScore = 0;
        for (PlayerResult res : results.values()) {
            if (res.isWon()) winners++;
            if (res.isFinished()) finishedCount++;
            totalScore += res.getPoints();
        }
        
        if (isActive) {
            return PuzzleStatsDTO.active(puzzleId, getRemainingTime(), results.size() - finishedCount, finishedCount, winners);
        } else {
            double avg = results.isEmpty() ? 0.0 : (double) totalScore / results.size();
            return PuzzleStatsDTO.concluded(puzzleId, results.size(), finishedCount, winners, avg);
        }
    }
}
