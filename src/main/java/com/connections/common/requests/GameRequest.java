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
        if (op == Operation.SUBMIT_PROPOSAL && (words == null || words.size() != 4)) {
            throw new IllegalArgumentException("A proposal must contain exactly 4 words.");
        }
        this.words = words;
    }

    // --- FACTORY METHODS ---

    /**
     * Creates a request to submit a word group proposal.
     * 
     * @param words the 4 words to propose
     * @return a new GameRequest
     * @throws IllegalArgumentException if the proposal does not contain exactly 4 words
     */
    public static GameRequest submitProposal(List<String> words) {
        return new GameRequest(Operation.SUBMIT_PROPOSAL, words);
    }

    // --- VALIDATION ---

    /**
     * Validates the request after deserialization.
     * <p>
     * This is necessary because serialization frameworks like Gson might bypass constructors when
     * creating objects from JSON.
     * </p>
     * 
     * @return true if the request state is valid, false otherwise
     */
    public boolean isValid() {
        if (getOperation() == Operation.SUBMIT_PROPOSAL) {
            return words != null && words.size() == 4;
        }
        return true;
    }

    // --- GETTERS ---

    public List<String> getWords() {
        return words;
    }
}
