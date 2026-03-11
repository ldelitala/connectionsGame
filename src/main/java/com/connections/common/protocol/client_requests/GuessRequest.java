package com.connections.common.protocol.client_requests;

import java.util.List;

/**
 * Request subclass for active puzzle operations.
 * Handles proposals, current puzzle info, and current puzzle stats.
 * The puzzle context is implicit (current puzzle for the user).
 */
public class GuessRequest extends ClientRequest {
    private final List<String> words;

    private GuessRequest(Operation op, List<String> words) {
        super(op);
        this.words = words;
    }

    /**
     * Creates a request to submit a word group proposal for the current puzzle.
     */
    public static GuessRequest submitProposal(List<String> words) {
        return new GuessRequest(Operation.SUBMIT_PROPOSAL, words);
    }

    /**
     * Creates a request for info about the current puzzle.
     */
    public static GuessRequest getCurrentInfo() {
        return new GuessRequest(Operation.GET_GAME_INFO, null);
    }

    /**
     * Creates a request for stats about the current puzzle.
     */
    public static GuessRequest getCurrentStats() {
        return new GuessRequest(Operation.GET_GAME_STATS, null);
    }

    public List<String> getWords() { return words; }

    public boolean isValid() {
        if (getOperation() == Operation.SUBMIT_PROPOSAL) {
            return words != null && !words.isEmpty();
        }
        return true;
    }
}
