package com.connections.server;

import com.connections.server.models.Game;
import com.connections.server.models.User;
import com.connections.common.models.WordGroup;
import com.connections.common.notifications.*;
import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import com.connections.common.dto.*;
import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages the active game session and player progress.
 */
public class GameSessionManager {
    private final GamesDAO gamesDAO;
    private final UserManager userManager;
    private final UdpNotifier udpNotifier;
    private final long gameDurationMillis;
    private Game currentGame;
    private long gameEndTime;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, List<WordGroup>> playerProgress = new ConcurrentHashMap<>();
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

        // Notify participants about game end
        if (udpNotifier != null) {
            Gson gson = new Gson();
            // 1. Global stats & leaderboard
            GameStatsDTO stats = getGlobalGameStats(gameId);
            
            // Create leaderboard entries
            List<User> allUsers = userManager.getAllUsers();
            List<LeaderboardEntryDTO> leaderboardEntries = new java.util.ArrayList<>();
            for (int i = 0; i < Math.min(10, allUsers.size()); i++) {
                User u = allUsers.get(i);
                leaderboardEntries.add(LeaderboardEntryDTO.create(i + 1, u.getUsername(), u.getTotalScore()));
            }
            LeaderboardDTO leaderboard = LeaderboardDTO.create(leaderboardEntries);
            
            String globalMsg = gson.toJson(GameEndNotification.create(leaderboard, stats));
            udpNotifier.notifyAll(globalMsg);

            // 2. Personal summaries for each participant
            for (String user : participants) {
                GameInfoDTO personal = getPersonalGameStatus(user, gameId);
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
        System.out.println("New game started: ID " + currentGame.getGameId());
        if (udpNotifier != null) {
            NewGameNotification notification = NewGameNotification.create(currentGame.getGameId(), currentGame.getAllWords());
            udpNotifier.notifyAll(new com.google.gson.Gson().toJson(notification));
        }
        scheduler.schedule(this::startNewGame, gameDurationMillis, TimeUnit.MILLISECONDS);
    }

    public Game getCurrentGame() {
        return currentGame;
    }

    public long getRemainingTime() {
        return Math.max(0, gameEndTime - System.currentTimeMillis());
    }

    public synchronized GameInfoDTO getPersonalGameStatus(String username, int gameId) {
        if (currentGame != null && gameId == currentGame.getGameId()) {
            PlayerResult res = getPlayerSnapshot(username);
            List<WordGroup> foundGroups = playerProgress.getOrDefault(username, Collections.emptyList());
            List<String> foundWords = foundGroups.stream()
                    .flatMap(g -> g.getWords().stream())
                    .collect(Collectors.toList());
            List<String> remainingWords = currentGame.getAllWords().stream()
                    .filter(w -> !foundWords.contains(w))
                    .collect(Collectors.toList());
            
            return GameInfoDTO.current(
                gameId, res.getMistakes(), res.getPoints(), res.isFinished(), res.isWon(),
                getRemainingTime(), foundGroups, remainingWords
            );
        } 
        Map<String, PlayerResult> history = gameHistory.get(gameId);
        if (history != null && history.containsKey(username)) {
            PlayerResult res = history.get(username);
            List<WordGroup> allGroups = null;
            Game oldGame = gamesDAO.getGame(gameId);
            if (oldGame != null) allGroups = oldGame.getGroups();
            
            return GameInfoDTO.past(gameId, res.getMistakes(), res.getPoints(), res.isFinished(), res.isWon(), allGroups);
        }
        return null;
    }

    public synchronized GameStatsDTO getGlobalGameStats(int gameId) {
        Map<String, PlayerResult> results;
        boolean isCurrent = false;
        if (currentGame != null && gameId == currentGame.getGameId()) {
            isCurrent = true;
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
        int finished = 0;
        int totalScore = 0;
        for (PlayerResult res : results.values()) {
            if (res.isWon()) winners++;
            if (res.isFinished()) finished++;
            totalScore += res.getPoints();
        }
        if (isCurrent) {
            return GameStatsDTO.current(gameId, getRemainingTime(), results.size() - finished, finished, winners);
        } else {
            double avg = results.isEmpty() ? 0.0 : (double) totalScore / results.size();
            return GameStatsDTO.past(gameId, results.size(), finished, winners, avg);
        }
    }

    /**
     * Submits a word proposal. Returns null if the request is invalid (game finished, time up).
     */
    public synchronized ProposalResultDTO submitProposal(String username, List<String> words) {
        if (completedPlayers.contains(username)) return null;
        if (getRemainingTime() <= 0) return null;
        if (words == null || words.size() != 4) return null;
        
        WordGroup matched = currentGame.getGroups().stream()
                .filter(g -> new HashSet<>(g.getWords()).equals(new HashSet<>(words)))
                .findFirst().orElse(null);

        if (matched != null) {
            List<WordGroup> progress = playerProgress.computeIfAbsent(username, k -> new ArrayList<>());
            if (progress.contains(matched)) return null; // Or return a "wrong" with 0 delta? User said "invalid request" cases
            
            progress.add(matched);
            boolean win = (progress.size() == 3);
            if (win) finalizePlayerGame(username, true);
            
            PlayerResult snapshot = getPlayerSnapshot(username);
            return ProposalResultDTO.success(6.0f, snapshot.getPoints());
        } else {
            int m = playerMistakes.getOrDefault(username, 0) + 1;
            playerMistakes.put(username, m);
            boolean lose = (m >= 4);
            if (lose) finalizePlayerGame(username, false);
            
            PlayerResult snapshot = getPlayerSnapshot(username);
            return ProposalResultDTO.wrong(-4.0f, snapshot.getPoints());
        }
    }

    private void finalizePlayerGame(String username, boolean won) {
        completedPlayers.add(username);
        PlayerResult res = getPlayerSnapshot(username);
        if (won) userManager.getUser(username).recordWin(res.getMistakes(), res.getPoints());
        else userManager.getUser(username).recordLoss(res.getMistakes(), res.getPoints());
    }

    public List<WordGroup> getPlayerProgress(String username) { return playerProgress.getOrDefault(username, Collections.emptyList()); }
    public int getPlayerMistakes(String username) { return playerMistakes.getOrDefault(username, 0); }
}
