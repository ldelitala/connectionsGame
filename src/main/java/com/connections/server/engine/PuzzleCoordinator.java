package com.connections.server.engine;

import com.connections.common.models.Category;
import com.connections.common.protocol.Operation;
import com.connections.common.protocol.client_requests.GameplayRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.dto.*;
import com.connections.server.models.*;
import com.connections.server.network.UdpNotifier;
import com.connections.server.persistence.PuzzlesDAO;
import com.connections.server.persistence.ServerStateDAO;

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
    private final ServerStateDAO stateDAO;
    private final long puzzleDurationMillis;
    private final BiConsumer<Integer, Map<String, PlayerResult>> onPuzzleEnd;
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private Puzzle currentPuzzle;
    private long puzzleStartTime;
    
    private final Map<String, Integer> playerMistakes = new ConcurrentHashMap<>();
    private final Set<String> completedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, List<List<String>>> playerCorrectGuesses = new ConcurrentHashMap<>();

    public PuzzleCoordinator(PuzzlesDAO puzzlesDAO, UserCoordinator userCoordinator, 
                             UdpNotifier udpNotifier, ServerStateDAO stateDAO,
                             long puzzleDurationMillis,
                             BiConsumer<Integer, Map<String, PlayerResult>> onPuzzleEnd) {
        this.puzzlesDAO = puzzlesDAO;
        this.userCoordinator = userCoordinator;
        this.udpNotifier = udpNotifier;
        this.stateDAO = stateDAO;
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

    synchronized void loadNextPuzzle() {
        this.currentPuzzle = puzzlesDAO.getNextPuzzle();
        this.puzzleStartTime = System.currentTimeMillis();
        playerMistakes.clear();
        completedPlayers.clear();
        playerCorrectGuesses.clear();
        
        // Persist the rotation state so we can resume after a restart
        if (stateDAO != null) {
            stateDAO.saveNextPuzzleIndex(puzzlesDAO.getNextPuzzleIndex());
        }

        if (currentPuzzle != null) {
            System.out.println("Starting new puzzle: " + currentPuzzle.getPuzzleId());
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
            // Find canonical words from the puzzle definition for correct removal later
            List<String> canonicalWords = words;
            for (Category cat : currentPuzzle.getGroups()) {
                if (cat.getWords().size() == words.size()) {
                    boolean match = true;
                    for (String w : words) {
                        boolean foundInCat = false;
                        for (String cw : cat.getWords()) {
                            if (cw.equalsIgnoreCase(w)) { foundInCat = true; break; }
                        }
                        if (!foundInCat) { match = false; break; }
                    }
                    if (match) { canonicalWords = cat.getWords(); break; }
                }
            }

            List<List<String>> correctOnes = playerCorrectGuesses.computeIfAbsent(username, k -> new ArrayList<>());
            for (List<String> found : correctOnes) {
                // Case-insensitive duplicate check
                boolean sameSet = true;
                for (String f : found) {
                    boolean foundInWords = false;
                    for (String w : words) {
                        if (w.equalsIgnoreCase(f)) { foundInWords = true; break; }
                    }
                    if (!foundInWords) { sameSet = false; break; }
                }
                if (sameSet) return null; 
            }
            correctOnes.add(canonicalWords); // Store canonical words (uppercase)
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
        int totalScore;
        if (u != null) {
            if (completedPlayers.contains(username)) {
                // If finished, u.getTotalScore() already includes current puzzle points
                totalScore = u.getTotalScore();
            } else {
                // If still playing, sum career + current session points
                totalScore = u.getTotalScore() + createPlayerSnapshot(username).getPoints();
            }
        } else {
            totalScore = createPlayerSnapshot(username).getPoints();
        }
        
        return GuessResultDTO.create(status, pointsDelta, totalScore, getRemainingWords(username));
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
            PlayerResult res = createPlayerSnapshot(username);
            
            int totalScore;
            if (u != null) {
                if (completedPlayers.contains(username)) {
                    totalScore = u.getTotalScore();
                } else {
                    totalScore = u.getTotalScore() + res.getPoints();
                }
            } else {
                totalScore = res.getPoints();
            }

            return PuzzleStateDTO.create(
                puzzleId, 
                completedPlayers.contains(username),
                remaining,
                currentPuzzle.getCategoriesForGroups(playerCorrectGuesses.getOrDefault(username, Collections.emptyList())),
                getRemainingWords(username),
                playerMistakes.getOrDefault(username, 0),
                totalScore
            );
        }
        return null;
    }

    public PuzzleStatsDTO getGlobalStats(int puzzleId) {
        if (currentPuzzle != null && currentPuzzle.getPuzzleId() == puzzleId) {
            Set<String> participants = new HashSet<>(playerMistakes.keySet());
            participants.addAll(playerCorrectGuesses.keySet());
            participants.addAll(completedPlayers);

            int total = participants.size();
            int finished = completedPlayers.size();
            int active = total - finished;
            
            long wonCount = completedPlayers.stream()
                .filter(u -> playerCorrectGuesses.getOrDefault(u, Collections.emptyList()).size() >= 3)
                .count();
            long remaining = Math.max(0, puzzleDurationMillis - (System.currentTimeMillis() - puzzleStartTime));
            return PuzzleStatsDTO.active(puzzleId, remaining, active, finished, (int)wonCount);
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
