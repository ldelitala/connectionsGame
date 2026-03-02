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
        Map<String, PlayerResult> results = new HashMap<>();
        Set<String> participants = new HashSet<>(playerProgress.keySet());
        participants.addAll(playerMistakes.keySet());
        for (String user : participants) {
            results.put(user, getPlayerSnapshot(user));
        }
        gameHistory.put(currentGame.getGameId(), results);
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
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "new_game");
            msg.put("gameId", currentGame.getGameId());
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
            return new CurrentGameInfoDTO(
                gameId, res.getMistakes(), res.getPoints(), res.isFinished(),
                getRemainingTime(), foundGroups, remainingWords
            );
        } 
        Map<String, PlayerResult> history = gameHistory.get(gameId);
        if (history != null && history.containsKey(username)) {
            PlayerResult res = history.get(username);
            List<WordGroup> allGroups = null;
            Game oldGame = gamesDAO.getGame(gameId);
            if (oldGame != null) allGroups = oldGame.getGroups();
            return new PastGameInfoDTO(gameId, res.getMistakes(), res.getPoints(), res.isFinished(), res.isWon(), allGroups);
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
                results.put(user, getPlayerSnapshot(user));
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
            if (res.isWon()) winners++;
            if (res.isFinished()) finished++;
            totalScore += res.getPoints();
        }
        Integer participantsCount = null;
        Double averageScore = null;
        if (isCurrent) {
            inProgressCount = results.size() - finished;
        } else {
            participantsCount = results.size();
            averageScore = participantsCount > 0 ? (double) totalScore / participantsCount : 0.0;
        }
        return new GameStatsDTO(gameId, isCurrent, remainingTime, inProgressCount, finished, winners, participantsCount, averageScore);
    }

    public synchronized ProposalResultDTO submitProposal(String username, List<String> words) {
        if (completedPlayers.contains(username)) return new ProposalResultDTO("error", "Finished.", null, null, null, null);
        if (getRemainingTime() <= 0) return new ProposalResultDTO("error", "Time up.", null, null, null, null);
        WordGroup matched = currentGame.getGroups().stream()
                .filter(g -> new HashSet<>(g.getWords()).equals(new HashSet<>(words)))
                .findFirst().orElse(null);
        if (matched != null) {
            List<WordGroup> progress = playerProgress.computeIfAbsent(username, k -> new ArrayList<>());
            if (progress.contains(matched)) return new ProposalResultDTO("error", "Already found.", null, null, null, null);
            progress.add(matched);
            boolean win = (progress.size() == 3);
            if (win) finalizePlayerGame(username, true);
            PlayerResult res = getPlayerSnapshot(username);
            return new ProposalResultDTO("ok", "Correct!", progress.size(), null, win, win ? res.getPoints() : null);
        } else {
            int m = playerMistakes.getOrDefault(username, 0) + 1;
            playerMistakes.put(username, m);
            boolean lose = (m >= 4);
            if (lose) finalizePlayerGame(username, false);
            PlayerResult res = getPlayerSnapshot(username);
            return new ProposalResultDTO("wrong", "Wrong!", null, m, lose, lose ? res.getPoints() : null);
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
