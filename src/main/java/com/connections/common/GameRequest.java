package com.connections.common;

import java.util.List;

/**
 * Implementation for Game management operations (submitProposal).
 */
public class GameRequest extends Request {
    private final List<String> words;

    public GameRequest(Operation op, List<String> words) {
        super(op);
        this.words = words;
    }

    public List<String> getWords() {
        return words;
    }
}
