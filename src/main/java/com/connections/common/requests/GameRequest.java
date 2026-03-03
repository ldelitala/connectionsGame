package com.connections.common.requests;

import java.util.List;

/**
 * specialized Request subclass for game-related operations.
 * <p>
 * Primarily handles the submission of word group proposals.
 * </p>
 */
public class GameRequest extends Request {
    /** The list of words submitted in this request. */
    private final List<String> words;

    private GameRequest(Operation op, List<String> words) {
        super(op);
        if (op == Operation.SUBMIT_PROPOSAL && (words == null || words.isEmpty())) {
            throw new IllegalArgumentException("A proposal must contain at least one word.");
        }
        this.words = words;
    }

    // --- FACTORY METHODS ---

    /**
     * Creates a request to submit a word group proposal.
     * 
     * @param words the words to propose
     * @return a new GameRequest
     * @throws IllegalArgumentException if the proposal is null or empty
     */
    public static GameRequest submitProposal(List<String> words) {
        return new GameRequest(Operation.SUBMIT_PROPOSAL, words);
    }

    // --- VALIDATION ---

    /**
     * Validates the request after deserialization.
     * <p>
     * Ensures that a proposal contains at least one word. 
     * Further rule validation (e.g., exactly 4 words) is performed by the server.
     * </p>
     * 
     * @return true if the request state is valid, false otherwise
     */
    public boolean isValid() {
        if (getOperation() == Operation.SUBMIT_PROPOSAL) {
            return words != null && !words.isEmpty();
        }
        return true;
    }

    // --- GETTERS ---

    public List<String> getWords() {
        return words;
    }
}
