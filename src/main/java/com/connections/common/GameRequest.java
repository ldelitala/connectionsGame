package com.connections.common;

import java.util.List;

/**
 * specialized Request subclass for game-related operations.
 * <p>
 * Currently handles the submission of word group proposals.
 * </p>
 */
public class GameRequest extends Request {
    /** The list of 4 words being proposed as a group. */
    private final List<String> words;

    /**
     * Constructs a new GameRequest.
     * 
     * @param op the operation type (typically {@link Operation#SUBMIT_PROPOSAL})
     * @param words the 4 words to propose
     */
    public GameRequest(Operation op, List<String> words) {
        super(op);
        this.words = words;
    }

    /**
     * Gets the list of words in the proposal.
     * 
     * @return the proposed words
     */
    public List<String> getWords() {
        return words;
    }
}
