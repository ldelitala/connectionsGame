package com.connections.common.protocol.client_requests;

import java.util.List;

/**
 * Request subclass for active game operations.
 * Handles proposals, current game info, and current game stats.
 * The game context is implicit (current game for the user).
 */
public class GameRequest extends ClientRequest {
    private final List<String> words;

    private GameRequest(Operation op, List<String> words) {
        super(op);
        this.words = words;
    }

    /**
     * Creates a request to submit a word group proposal for the current game.
     */
    public static GameRequest submitProposal(List<String> words) {
        return new GameRequest(Operation.SUBMIT_PROPOSAL, words);
    }

    /**
     * Creates a request for info about the current game.
     */
    public static GameRequest getCurrentInfo() {
        return new GameRequest(Operation.GET_GAME_INFO, null);
    }

    /**
     * Creates a request for stats about the current game.
     */
    public static GameRequest getCurrentStats() {
        return new GameRequest(Operation.GET_GAME_STATS, null);
    }

    public List<String> getWords() { return words; }

    public boolean isValid() {
        if (getOperation() == Operation.SUBMIT_PROPOSAL) {
            return words != null && !words.isEmpty();
        }
        return true;
    }
}
