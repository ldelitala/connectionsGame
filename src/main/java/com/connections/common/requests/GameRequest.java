package com.connections.common.requests;

import java.util.List;

/**
 * specialized Request subclass for game-related operations.
 * <p>
 * Primarily handles the submission of word group proposals.
 * </p>
 */
public class GameRequest extends Request {
    private final List<String> words;

    private GameRequest(Operation op, List<String> words) {
        super(op);
        this.words = words;
    }

    // --- FACTORY METHODS ---

    /**
     * Creates a request to submit a word group proposal.
     * 
     * @param words the 4 words to propose
     * @return a new GameRequest
     */
    public static GameRequest submitProposal(List<String> words) {
        return new GameRequest(Operation.SUBMIT_PROPOSAL, words);
    }

    public List<String> getWords() {
        return words;
    }
}
