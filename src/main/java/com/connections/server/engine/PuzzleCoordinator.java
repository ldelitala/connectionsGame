package com.connections.server.engine;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.client_requests.GameplayRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.dto.*;
import com.connections.server.models.*;
import com.connections.server.network.UdpNotifier;
import com.connections.server.persistence.PuzzlesDAO;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Orchestrates the lifecycle of active puzzles and user interactions during a round.
 * Responsible for operations under the "GAMEPLAY" section of the protocol.
 */
public class PuzzleCoordinator {

    private final PuzzlesDAO puzzlesDAO;
    private final UserCoordinator userCoordinator;
    private final UdpNotifier udpNotifier;
    private final long puzzleDurationMillis;
    private final BiConsumer<Integer, Map<String, PlayerResult>> onPuzzleEnd;
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private Puzzle currentPuzzle;
    private long puzzleStartTime;
    
    private final Map<String, Integer> playerMistakes = new ConcurrentHashMap<>();
    private final Set<String> completedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, List<List<String>>> playerCorrectGuesses = new ConcurrentHashMap<>();

    public PuzzleCoordinator(PuzzlesDAO puzzlesDAO, UserCoordinator userCoordinator, 
                             UdpNotifier udpNotifier, long puzzleDurationMillis,
                             BiConsumer<Integer, Map<String, PlayerResult>> onPuzzleEnd) {
        this.puzzlesDAO = puzzlesDAO;
        this.userCoordinator = userCoordinator;
        this.udpNotifier = udpNotifier;
        this.puzzleDurationMillis = puzzleDurationMillis;
        this.onPuzzleEnd = onPuzzleEnd;
    }

    public void startRotation() {
        loadNextPuzzle();
        scheduler.scheduleAtFixedRate(this::finalizePuzzle, 
                                      puzzleDurationMillis, puzzleDurationMillis, 
                                      TimeUnit.MILLISECONDS);
    }

    /**
     * Handles gameplay requests defined in Operation.java.
     */
    public ServerResponse<?> handle(GameplayRequest req, Operation op, String username) {
        if (username == null) return ServerResponse.forbidden(op, "Login required to play.");
        
        switch (op) {
            case SUBMIT_PROPOSAL:
                GuessResultDTO result = handleProposal(username, req.getWords());
                return (result != null) ? ServerResponse.ok(op, "Proposal processed.", result) 
                                        : ServerResponse.error(op, "Invalid or late proposal.");

            case GET_PUZZLE_INFO:
                PuzzleStateDTO state = getPersonalStatus(username, currentPuzzle.getPuzzleId());
                return (state != null) ? ServerResponse.ok(op, state) : ServerResponse.error(op, "Active puzzle not found.");

            case GET_PUZZLE_STATS:
                PuzzleStatsDTO stats = getGlobalStats(currentPuzzle.getPuzzleId());
                return (stats != null) ? ServerResponse.ok(op, stats) : ServerResponse.error(op, "Global stats not available.");

            default: 
                return ServerResponse.error(op, "Unsupported gameplay operation: " + op);
        }
    }

    private synchronized void loadNextPuzzle() {
        this.currentPuzzle = puzzlesDAO.getRandomPuzzle();
        this.puzzleStartTime = System.currentTimeMillis();
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
            for (List<String> found : correctOnes) {
                if (new HashSet<>(found).equals(new HashSet<>(words))) return null; 
            }
            correctOnes.add(words);
            pointsDelta = currentPuzzle.getPointsForWords(words);
            status = "success";
            
            if (correctOnes.size() == 3) {
                completedPlayers.add(username);
                recordEndGameStats(username, true);
            }
        } else {
            mistakes++;
            playerMistakes.put(username, mistakes);
            pointsDelta = -4.0;
            status = "wrong";
            
            if (mistakes >= 4) {
                completedPlayers.add(username);
                recordEndGameStats(username, false);
            }
        }

        User u = userCoordinator.getUser(username);
        return GuessResultDTO.create(status, pointsDelta, u != null ? u.getTotalScore() : 0, getRemainingWords(username));
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

    public synchronized void finalizePuzzle() {
        if (currentPuzzle == null) return;
        
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

        if (onPuzzleEnd != null) {
            onPuzzleEnd.accept(currentPuzzle.getPuzzleId(), results);
        }

        udpNotifier.broadcastPuzzleEnd(currentPuzzle.getPuzzleId(), null, getGlobalStats(currentPuzzle.getPuzzleId()));
        
        for (String username : participants) {
            udpNotifier.sendPlayerSummary(username, getPersonalStatus(username, currentPuzzle.getPuzzleId()));
        }

        loadNextPuzzle();
    }

    public PuzzleStateDTO getPersonalStatus(String username, int puzzleId) {
        if (currentPuzzle != null && currentPuzzle.getPuzzleId() == puzzleId) {
            long remaining = Math.max(0, puzzleDurationMillis - (System.currentTimeMillis() - puzzleStartTime));
            User u = userCoordinator.getUser(username);
            return PuzzleStateDTO.create(
                puzzleId, 
                completedPlayers.contains(username),
                remaining,
                currentPuzzle.getCategoriesForGroups(playerCorrectGuesses.getOrDefault(username, Collections.emptyList())),
                getRemainingWords(username),
                playerMistakes.getOrDefault(username, 0),
                u != null ? u.getTotalScore() : 0
            );
        }
        return null;
    }

    public PuzzleStatsDTO getGlobalStats(int puzzleId) {
        if (currentPuzzle != null && currentPuzzle.getPuzzleId() == puzzleId) {
            int active = playerMistakes.size() - completedPlayers.size();
            long wonCount = completedPlayers.stream()
                .filter(u -> playerCorrectGuesses.getOrDefault(u, Collections.emptyList()).size() >= 3)
                .count();
            long remaining = Math.max(0, puzzleDurationMillis - (System.currentTimeMillis() - puzzleStartTime));
            return PuzzleStatsDTO.active(puzzleId, remaining, active, completedPlayers.size(), (int)wonCount);
        }
        return null;
    }

    private List<String> getRemainingWords(String username) {
        if (currentPuzzle == null) return Collections.emptyList();
        List<String> all = currentPuzzle.getAllWords();
        List<List<String>> found = playerCorrectGuesses.getOrDefault(username, Collections.emptyList());
        List<String> remaining = new ArrayList<>(all);
        for (List<String> group : found) {
            remaining.removeAll(group);
        }
        return remaining;
    }

    private PlayerResult createPlayerSnapshot(String username) {
        int correct = playerCorrectGuesses.getOrDefault(username, Collections.emptyList()).size();
        int mistakes = playerMistakes.getOrDefault(username, 0);
        return new PlayerResult(mistakes, correct, completedPlayers.contains(username));
    }
}
