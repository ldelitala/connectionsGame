package com.connections.common.protocol.requests;

import java.util.List;

/**
 * Request subclass for active game operations.
 * Handles proposals, current game info, and current game stats.
 */
public class GameRequest extends Request {
    private final List<String> words;
    private final Integer gameId;

    private GameRequest(Operation op, List<String> words, Integer gameId) {
        super(op);
        this.words = words;
        this.gameId = gameId;
    }

    /**
     * Creates a request to submit a word group proposal for the current game.
     */
    public static GameRequest submitProposal(List<String> words) {
        return new GameRequest(Operation.SUBMIT_PROPOSAL, words, null);
    }

    /**
     * Creates a request for info about the current game.
     */
    public static GameRequest getCurrentInfo() {
        return new GameRequest(Operation.GET_GAME_INFO, null, null);
    }

    /**
     * Creates a request for stats about the current game.
     */
    public static GameRequest getCurrentStats() {
        return new GameRequest(Operation.GET_GAME_STATS, null, null);
    }

    public List<String> getWords() { return words; }
    public Integer getGameId() { return gameId; }

    public boolean isValid() {
        if (getOperation() == Operation.SUBMIT_PROPOSAL) {
            return words != null && !words.isEmpty();
        }
        return true;
    }
}
