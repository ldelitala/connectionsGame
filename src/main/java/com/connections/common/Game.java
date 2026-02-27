package com.connections.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a game consisting of 16 words in 4 groups.
 */
public class Game {
    private int gameId;
    private List<WordGroup> groups;

    public Game(int gameId, List<WordGroup> groups) {
        this.gameId = gameId;
        this.groups = groups;
    }

    public int getGameId() {
        return gameId;
    }

    public List<WordGroup> getGroups() {
        return groups;
    }

    public List<String> getAllWords() {
        List<String> allWords = new ArrayList<>();
        for (WordGroup group : groups) {
            allWords.addAll(group.getWords());
        }
        return allWords;
    }
}
