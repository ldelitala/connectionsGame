package com.connections.server;

import com.connections.common.Game;
import com.connections.common.WordGroup;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the active game session and player progress.
 */
public class GameSessionManager {
    private final GameManager gameManager;
    private final UserManager userManager;
    private final UdpNotifier udpNotifier;
    private Game currentGame;
    private long gameEndTime;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // Tracks player progress in the current game: username -> list of correct word groups
    private final Map<String, List<WordGroup>> playerProgress = new ConcurrentHashMap<>();
    // Tracks player mistakes: username -> count
    private final Map<String, Integer> playerMistakes = new ConcurrentHashMap<>();
    // Tracks players who completed the game
    private final Set<String> completedPlayers = ConcurrentHashMap.newKeySet();

    public GameSessionManager(GameManager gameManager, UserManager userManager, UdpNotifier udpNotifier) {
        this.gameManager = gameManager;
        this.userManager = userManager;
        this.udpNotifier = udpNotifier;
        startNewGame();
    }

    private void startNewGame() {
        int numGames = gameManager.getNumGames();
        int gameId = new Random().nextInt(numGames);
        currentGame = gameManager.getGame(gameId);
        gameEndTime = System.currentTimeMillis() + 300000; // 5 minutes per game
        
        playerProgress.clear();
        playerMistakes.clear();
        completedPlayers.clear();
        
        System.out.println("New game started: ID " + gameId);
        
        if (udpNotifier != null) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "new_game");
            msg.put("gameId", gameId);
            msg.put("words", currentGame.getAllWords());
            udpNotifier.notifyAll(new com.google.gson.Gson().toJson(msg));
        }
        
        // Schedule next game
        scheduler.schedule(this::startNewGame, 5, TimeUnit.MINUTES);
    }

    // History: gameId -> (username -> Result)
    private final Map<Integer, Map<String, PlayerResult>> gameHistory = new ConcurrentHashMap<>();

    public static class PlayerResult {
        public int mistakes;
        public int correctGroups;
        public int points;
        public boolean won;
        public boolean finished;
    }

    public GameSessionManager(GameManager gameManager, UserManager userManager, UdpNotifier udpNotifier) {
        this.gameManager = gameManager;
        this.userManager = userManager;
        this.udpNotifier = udpNotifier;
        startNewGame();
    }

    private void archiveCurrentGame() {
        if (currentGame == null) return;
        Map<String, PlayerResult> results = new HashMap<>();
        // All players who participated in the current game
        Set<String> participants = new HashSet<>(playerProgress.keySet());
        participants.addAll(playerMistakes.keySet());
        
        for (String user : participants) {
            PlayerResult res = new PlayerResult();
            res.mistakes = playerMistakes.getOrDefault(user, 0);
            res.correctGroups = playerProgress.getOrDefault(user, Collections.emptyList()).size();
            res.points = calculatePoints(res.correctGroups, res.mistakes);
            res.finished = completedPlayers.contains(user);
            res.won = (res.correctGroups == 3);
            results.put(user, res);
        }
        gameHistory.put(currentGame.getGameId(), results);
        // TODO: Save to games_log.json
    }

    private void startNewGame() {
        archiveCurrentGame();
        currentGame = gameManager.getRandomGame();
        int gameId = currentGame.getGameId();
        gameEndTime = System.currentTimeMillis() + 300000; // 5 minutes per game
        
        playerProgress.clear();
        playerMistakes.clear();
        completedPlayers.clear();
        
        System.out.println("New game started: ID " + gameId);
        
        if (udpNotifier != null) {
            udpNotifier.notifyAll("A new game has started! ID: " + gameId);
            // In a real implementation, we would send the 16 words too
        }
        
        // Schedule next game
        scheduler.schedule(this::startNewGame, 5, TimeUnit.MINUTES);
    }

    public Game getCurrentGame() {
        return currentGame;
    }

    public long getRemainingTime() {
        return Math.max(0, gameEndTime - System.currentTimeMillis());
    }

    public synchronized Map<String, Object> getPersonalGameStatus(String username, int gameId) {
        Map<String, Object> status = new HashMap<>();
        if (currentGame != null && gameId == currentGame.getGameId()) {
            status.put("gameId", gameId);
            status.put("remainingTime", getRemainingTime());
            status.put("foundGroups", playerProgress.getOrDefault(username, Collections.emptyList()));
            status.put("mistakes", playerMistakes.getOrDefault(username, 0));
            status.put("currentScore", calculatePoints(playerProgress.getOrDefault(username, Collections.emptyList()).size(), playerMistakes.getOrDefault(username, 0)));
            status.put("finished", completedPlayers.contains(username));
            return status;
        } else {
            Map<String, PlayerResult> history = gameHistory.get(gameId);
            if (history != null && history.containsKey(username)) {
                PlayerResult res = history.get(username);
                status.put("gameId", gameId);
                status.put("mistakes", res.mistakes);
                status.put("foundGroupsCount", res.correctGroups);
                status.put("score", res.points);
                status.put("won", res.won);
                status.put("finished", res.finished);
                return status;
            }
        }
        return null;
    }

    public synchronized Map<String, Object> getGlobalGameStats(int gameId) {
        Map<String, PlayerResult> results;
        if (currentGame != null && gameId == currentGame.getGameId()) {
             // Calculate on the fly for current game
             results = new HashMap<>();
             Set<String> participants = new HashSet<>(playerProgress.keySet());
             participants.addAll(playerMistakes.keySet());
             for (String user : participants) {
                 PlayerResult res = new PlayerResult();
                 res.mistakes = playerMistakes.getOrDefault(user, 0);
                 res.correctGroups = playerProgress.getOrDefault(user, Collections.emptyList()).size();
                 res.points = calculatePoints(res.correctGroups, res.mistakes);
                 res.finished = completedPlayers.contains(user);
                 res.won = (res.correctGroups == 3);
                 results.put(user, res);
             }
        } else {
            results = gameHistory.get(gameId);
        }

        if (results == null) return null;

        int totalPlayers = results.size();
        int winners = 0;
        int losers = 0;
        int totalScore = 0;
        for (PlayerResult res : results.values()) {
            if (res.won) winners++;
            else if (res.finished) losers++; // If finished but not won, it's a loss
            totalScore += res.points;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("gameId", gameId);
        stats.put("totalPlayers", totalPlayers);
        stats.put("winners", winners);
        stats.put("losers", losers);
        stats.put("averageScore", totalPlayers > 0 ? (double) totalScore / totalPlayers : 0.0);
        return stats;
    }

    public synchronized Map<String, Object> submitProposal(String username, List<String> words) {
        if (completedPlayers.contains(username)) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", "You already finished this game.");
            return err;
        }
        
        if (getRemainingTime() <= 0) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", "Time is up!");
            return err;
        }

        // Validate words belong to the current game
        List<String> allWords = currentGame.getAllWords();
        if (!allWords.containsAll(words)) {
             Map<String, Object> err = new HashMap<>();
             err.put("status", "error");
             err.put("message", "Words not in current game.");
             return err;
        }

        // Find matching group
        WordGroup matchedGroup = null;
        for (WordGroup group : currentGame.getGroups()) {
            if (new HashSet<>(group.getWords()).equals(new HashSet<>(words))) {
                matchedGroup = group;
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (matchedGroup != null) {
            List<WordGroup> progress = playerProgress.computeIfAbsent(username, k -> new ArrayList<>());
            if (progress.contains(matchedGroup)) {
                Map<String, Object> err = new HashMap<>();
                err.put("status", "error");
                err.put("message", "Group already found.");
                return err;
            }
            progress.add(matchedGroup);
            result.put("status", "ok");
            result.put("message", "Correct! Group: " + matchedGroup.getTheme());
            result.put("foundGroups", progress.size());
            
            if (progress.size() == 3) { // 4th is implicit
                completedPlayers.add(username);
                int mistakes = playerMistakes.getOrDefault(username, 0);
                int points = calculatePoints(3, mistakes);
                userManager.getUser(username).recordWin(mistakes, points);
                result.put("gameFinished", true);
                result.put("finalPoints", points);
            }
        } else {
            int mistakes = playerMistakes.getOrDefault(username, 0) + 1;
            playerMistakes.put(username, mistakes);
            result.put("status", "wrong");
            result.put("message", "Incorrect proposal.");
            result.put("mistakes", mistakes);
            
            if (mistakes >= 4) {
                completedPlayers.add(username);
                int points = calculatePoints(playerProgress.getOrDefault(username, Collections.emptyList()).size(), mistakes);
                userManager.getUser(username).recordLoss(mistakes, points);
                result.put("gameFinished", true);
                result.put("finalPoints", points);
            }
        }
        
        return result;
    }

    private int calculatePoints(int correctGroups, int mistakes) {
        int points = 0;
        if (correctGroups == 1) points = 6;
        else if (correctGroups == 2) points = 12;
        else if (correctGroups == 3) points = 18;
        
        points -= (mistakes * 4);
        return points;
    }

    public List<WordGroup> getPlayerProgress(String username) {
        return playerProgress.getOrDefault(username, Collections.emptyList());
    }

    public int getPlayerMistakes(String username) {
        return playerMistakes.getOrDefault(username, 0);
    }
}
