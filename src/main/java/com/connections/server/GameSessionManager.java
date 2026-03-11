package com.connections.server;

import com.connections.server.models.Puzzle;
import com.connections.server.models.User;
import com.connections.common.models.Category;
import com.connections.common.notifications.*;
import com.connections.common.dto.*;
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
public class GameSessionManager {

    // --- Scoring Rules ---
    private static final int POINTS_PER_GROUP = 6;
    private static final int PENALTY_PER_MISTAKE = 4;
    private static final int MAX_MISTAKES = 4;
    private static final int GROUPS_TO_WIN = 3; // 4th is implicit

    // --- Configuration ---
    private static final int TOP_K_LEADERBOARD = 10;
    private static final int WORDS_PER_PROPOSAL = 4;

    private final GamesDAO gamesDAO;
    private final UserManager userManager;
    private final UdpNotifier udpNotifier;
    private final long gameDurationMillis;
    private Puzzle currentGame;
    private long gameEndTime;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, List<Category>> playerProgress = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerMistakes = new ConcurrentHashMap<>();
    private final Set<String> completedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Map<String, PlayerResult>> gameHistory = new ConcurrentHashMap<>();

    public GameSessionManager(GamesDAO gamesDAO, UserManager userManager, UdpNotifier udpNotifier, long gameDurationMillis) {
        this.gamesDAO = gamesDAO;
        this.userManager = userManager;
        this.udpNotifier = udpNotifier;
        this.gameDurationMillis = gameDurationMillis;
        startNewGame();
    }

    private PlayerResult getPlayerSnapshot(String username) {
        int mistakes = playerMistakes.getOrDefault(username, 0);
        int correctGroups = playerProgress.getOrDefault(username, Collections.emptyList()).size();
        boolean finished = completedPlayers.contains(username);
        int score = (correctGroups * POINTS_PER_GROUP) - (mistakes * PENALTY_PER_MISTAKE);
        return new PlayerResult(mistakes, correctGroups, finished);
    }

    private void archiveCurrentGame() {
        if (currentGame == null) return;
        int gameId = currentGame.getGameId();
        Map<String, PlayerResult> results = new HashMap<>();
        
        Set<String> participants = new HashSet<>(playerProgress.keySet());
        participants.addAll(playerMistakes.keySet());
        
        for (String user : participants) {
            results.put(user, getPlayerSnapshot(user));
        }
        gameHistory.put(gameId, results);

        if (udpNotifier != null) {
            Gson gson = new Gson();
            PuzzleStatsDTO stats = getGlobalGameStats(gameId);
            List<User> topPlayers = userManager.getTopK(TOP_K_LEADERBOARD);
            List<LeaderboardEntryDTO> leaderboardEntries = new ArrayList<>();
            for (int i = 0; i < topPlayers.size(); i++) {
                User u = topPlayers.get(i);
                leaderboardEntries.add(LeaderboardEntryDTO.create(i + 1, u.getUsername(), u.getTotalScore()));
            }
            LeaderboardDTO leaderboard = LeaderboardDTO.create(leaderboardEntries);
            
            String globalMsg = gson.toJson(GameEndNotification.create(leaderboard, stats));
            udpNotifier.notifyAll(globalMsg);

            for (String user : participants) {
                PuzzleStateDTO personal = getPersonalGameStatus(user, gameId);
                if (personal != null) {
                    String personalMsg = gson.toJson(PlayerGameSummaryNotification.create(personal));
                    udpNotifier.notifyUser(user, personalMsg);
                }
            }
        }
    }

    private void startNewGame() {
        archiveCurrentGame();
        currentGame = gamesDAO.getNextGame();
        if (currentGame == null) {
            System.err.println("No games available!");
            return;
        }
        gameEndTime = System.currentTimeMillis() + gameDurationMillis;
        playerProgress.clear();
        playerMistakes.clear();
        completedPlayers.clear();
        
        System.out.println(String.format("New puzzle started: ID %d", currentGame.getGameId()));
        if (udpNotifier != null) {
            NewGameNotification notification = NewGameNotification.create(currentGame.getGameId(), currentGame.getAllWords());
            udpNotifier.notifyAll(new Gson().toJson(notification));
        }
        scheduler.schedule(this::startNewGame, gameDurationMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized GuessResultDTO submitProposal(String username, List<String> words) {
        if (completedPlayers.contains(username)) return null;
        if (getRemainingTime() <= 0) return null;
        if (words == null || words.size() != WORDS_PER_PROPOSAL) return null;
        
        Category matched = currentGame.getGroups().stream()
                .filter(g -> new HashSet<>(g.getWords()).equals(new HashSet<>(words)))
                .findFirst().orElse(null);

        if (matched != null) {
            List<Category> progress = playerProgress.computeIfAbsent(username, k -> new ArrayList<>());
            if (progress.contains(matched)) return null; 
            
            progress.add(matched);
            boolean win = (progress.size() == GROUPS_TO_WIN); 
            if (win) finalizePlayerGame(username, true);
            
            PlayerResult snapshot = getPlayerSnapshot(username);
            return GuessResultDTO.success((float) POINTS_PER_GROUP, snapshot.getPoints());
        } else {
            int m = playerMistakes.getOrDefault(username, 0) + 1;
            playerMistakes.put(username, m);
            boolean lose = (m >= MAX_MISTAKES);
            if (lose) finalizePlayerGame(username, false);
            
            PlayerResult snapshot = getPlayerSnapshot(username);
            return GuessResultDTO.wrong((float) -PENALTY_PER_MISTAKE, snapshot.getPoints());
        }
    }

    private void finalizePlayerGame(String username, boolean won) {
        completedPlayers.add(username);
        PlayerResult res = getPlayerSnapshot(username);
        User u = userManager.getUser(username);
        if (won) u.recordWin(res.getMistakes(), res.getPoints());
        else u.recordLoss(res.getMistakes(), res.getPoints());
    }

    public long getRemainingTime() {
        return Math.max(0, gameEndTime - System.currentTimeMillis());
    }

    public Puzzle getCurrentGame() { return currentGame; }
    public List<Category> getPlayerProgress(String username) { return playerProgress.getOrDefault(username, Collections.emptyList()); }
    public int getPlayerMistakes(String username) { return playerMistakes.getOrDefault(username, 0); }

    public synchronized PuzzleStateDTO getPersonalGameStatus(String username, int gameId) {
        if (currentGame != null && gameId == currentGame.getGameId()) {
            PlayerResult res = getPlayerSnapshot(username);
            List<Category> foundGroups = playerProgress.getOrDefault(username, Collections.emptyList());
            List<String> foundWords = foundGroups.stream()
                    .flatMap(g -> g.getWords().stream())
                    .collect(Collectors.toList());
            List<String> remainingWords = currentGame.getAllWords().stream()
                    .filter(w -> !foundWords.contains(w))
                    .collect(Collectors.toList());
            
            return PuzzleStateDTO.active(gameId, getRemainingTime(), foundGroups, remainingWords, res.getMistakes(), res.getPoints());
        } 
        
        Map<String, PlayerResult> history = gameHistory.get(gameId);
        if (history != null && history.containsKey(username)) {
            PlayerResult res = history.get(username);
            Puzzle oldGame = gamesDAO.getGame(gameId);
            List<Category> allGroups = (oldGame != null) ? oldGame.getGroups() : Collections.emptyList();
            
            return PuzzleStateDTO.concluded(gameId, allGroups, res.getCorrectGroups(), res.getMistakes(), res.getPoints());
        }
        return null;
    }

    public synchronized PuzzleStatsDTO getGlobalGameStats(int gameId) {
        Map<String, PlayerResult> results;
        boolean isActive = false;
        
        if (currentGame != null && gameId == currentGame.getGameId()) {
            isActive = true;
            results = new HashMap<>();
            Set<String> participants = new HashSet<>(playerProgress.keySet());
            participants.addAll(playerMistakes.keySet());
            for (String user : participants) {
                results.put(user, getPlayerSnapshot(user));
            }
        } else {
            results = gameHistory.get(gameId);
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
            return PuzzleStatsDTO.active(gameId, getRemainingTime(), results.size() - finishedCount, finishedCount, winners);
        } else {
            double avg = results.isEmpty() ? 0.0 : (double) totalScore / results.size();
            return PuzzleStatsDTO.concluded(gameId, results.size(), finishedCount, winners, avg);
        }
    }
}
