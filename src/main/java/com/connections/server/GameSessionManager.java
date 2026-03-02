package com.connections.server;

import com.connections.common.Game;
import com.connections.common.WordGroup;
import com.connections.common.dto.*;

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

    public static class PlayerResult {
        public int mistakes;
        public int correctGroups;
        public int points;
        public boolean won;
        public boolean finished;
    }

    public GameSessionManager(GamesDAO gamesDAO, UserManager userManager, UdpNotifier udpNotifier, long gameDurationMillis) {
        this.gamesDAO = gamesDAO;
        this.userManager = userManager;
        this.udpNotifier = udpNotifier;
        this.gameDurationMillis = gameDurationMillis;
        startNewGame();
    }

    private PlayerResult calculatePlayerResult(String username) {
        PlayerResult res = new PlayerResult();
        res.mistakes = playerMistakes.getOrDefault(username, 0);
        res.correctGroups = playerProgress.getOrDefault(username, Collections.emptyList()).size();
        res.points = calculatePoints(res.correctGroups, res.mistakes);
        res.finished = completedPlayers.contains(username);
        res.won = (res.correctGroups == 3);
        return res;
    }

    private void archiveCurrentGame() {
        if (currentGame == null) return;
        Map<String, PlayerResult> results = new HashMap<>();
        Set<String> participants = new HashSet<>(playerProgress.keySet());
        participants.addAll(playerMistakes.keySet());
        for (String user : participants) {
            results.put(user, calculatePlayerResult(user));
        }
        gameHistory.put(currentGame.getGameId(), results);
    }

    private void startNewGame() {
        archiveCurrentGame();
        currentGame = gamesDAO.getNextGame();
        if (currentGame == null) {
            System.err.println("No games available in GamesDAO!");
            return;
        }
        int gameId = currentGame.getGameId();
        gameEndTime = System.currentTimeMillis() + gameDurationMillis;
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
        scheduler.schedule(this::startNewGame, gameDurationMillis, TimeUnit.MILLISECONDS);
    }

    public Game getCurrentGame() {
        return currentGame;
    }

    public long getRemainingTime() {
        return Math.max(0, gameEndTime - System.currentTimeMillis());
    }

    public synchronized GameStatusDTO getPersonalGameStatus(String username, int gameId) {
        if (currentGame != null && gameId == currentGame.getGameId()) {
            PlayerResult res = calculatePlayerResult(username);
            List<WordGroup> foundGroups = playerProgress.getOrDefault(username, Collections.emptyList());
            
            List<String> foundWords = foundGroups.stream()
                    .flatMap(g -> g.getWords().stream())
                    .collect(Collectors.toList());
            
            List<String> remainingWords = currentGame.getAllWords().stream()
                    .filter(w -> !foundWords.contains(w))
                    .collect(Collectors.toList());
            
            return new CurrentGameStatusDTO(
                gameId, res.mistakes, res.points, res.finished,
                getRemainingTime(), foundGroups, remainingWords
            );
        } 
        
        Map<String, PlayerResult> history = gameHistory.get(gameId);
        if (history != null && history.containsKey(username)) {
            PlayerResult res = history.get(username);
            List<WordGroup> allGroups = null;
            Game oldGame = gamesDAO.getGame(gameId);
            if (oldGame != null) {
                allGroups = oldGame.getGroups();
            }
            return new PastGameStatusDTO(gameId, res.mistakes, res.points, res.finished, res.won, allGroups);
        }
        return null;
    }

    public synchronized GameStatsDTO getGlobalGameStats(int gameId) {
        Map<String, PlayerResult> results;
        boolean isCurrent = false;
        Long remainingTime = null;
        Integer inProgressCount = null;

        if (currentGame != null && gameId == currentGame.getGameId()) {
            isCurrent = true;
            remainingTime = getRemainingTime();
            results = new HashMap<>();
            Set<String> participants = new HashSet<>(playerProgress.keySet());
            participants.addAll(playerMistakes.keySet());
            for (String user : participants) {
                results.put(user, calculatePlayerResult(user));
            }
        } else {
            results = gameHistory.get(gameId);
        }

        if (results == null) return null;

        int totalParticipants = results.size();
        int winners = 0;
        int finished = 0;
        int totalScore = 0;

        for (PlayerResult res : results.values()) {
            if (res.won) winners++;
            if (res.finished) finished++;
            totalScore += res.points;
        }

        Integer totalParticipantsToReturn = null;
        Double averageScore = null;

        if (isCurrent) {
            inProgressCount = totalParticipants - finished;
        } else {
            totalParticipantsToReturn = totalParticipants;
            averageScore = totalParticipants > 0 ? (double) totalScore / totalParticipants : 0.0;
        }

        return new GameStatsDTO(
            gameId, isCurrent, remainingTime, inProgressCount,
            finished, winners, totalParticipantsToReturn, averageScore
        );
    }

    public synchronized ProposalResultDTO submitProposal(String username, List<String> words) {
        if (completedPlayers.contains(username)) {
            return new ProposalResultDTO("error", "You already finished this game.", null, null, null, null);
        }

        if (getRemainingTime() <= 0) {
            return new ProposalResultDTO("error", "Time is up!", null, null, null, null);
        }

        if (!currentGame.getAllWords().containsAll(words)) {
            return new ProposalResultDTO("error", "Words not in current game.", null, null, null, null);
        }

        WordGroup matchedGroup = currentGame.getGroups().stream()
                .filter(g -> new HashSet<>(g.getWords()).equals(new HashSet<>(words)))
                .findFirst().orElse(null);

        if (matchedGroup != null) {
            List<WordGroup> progress = playerProgress.computeIfAbsent(username, k -> new ArrayList<>());
            if (progress.contains(matchedGroup)) {
                return new ProposalResultDTO("error", "Group already found.", null, null, null, null);
            }
            progress.add(matchedGroup);
            
            boolean gameFinished = (progress.size() == 3);
            Integer finalPoints = null;
            if (gameFinished) {
                finalizePlayerGame(username, true);
                finalPoints = calculatePlayerResult(username).points;
            }
            
            return new ProposalResultDTO("ok", "Correct! Group: " + matchedGroup.getTheme(), progress.size(), null, gameFinished, finalPoints);
        } else {
            int mistakes = playerMistakes.getOrDefault(username, 0) + 1;
            playerMistakes.put(username, mistakes);
            
            boolean gameFinished = (mistakes >= 4);
            Integer finalPoints = null;
            if (gameFinished) {
                finalizePlayerGame(username, false);
                finalPoints = calculatePlayerResult(username).points;
            }
            
            return new ProposalResultDTO("wrong", "Incorrect proposal.", null, mistakes, gameFinished, finalPoints);
        }
    }

    private void finalizePlayerGame(String username, boolean won) {
        completedPlayers.add(username);
        PlayerResult res = calculatePlayerResult(username);
        if (won) {
            userManager.getUser(username).recordWin(res.mistakes, res.points);
        } else {
            userManager.getUser(username).recordLoss(res.mistakes, res.points);
        }
    }

    private int calculatePoints(int correctGroups, int mistakes) {
        int points = (correctGroups == 1) ? 6 : (correctGroups == 2) ? 12 : (correctGroups == 3) ? 18 : 0;
        return points - (mistakes * 4);
    }

    public List<WordGroup> getPlayerProgress(String username) {
        return playerProgress.getOrDefault(username, Collections.emptyList());
    }

    public int getPlayerMistakes(String username) {
        return playerMistakes.getOrDefault(username, 0);
    }
}
