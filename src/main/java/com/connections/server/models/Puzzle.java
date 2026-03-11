package com.connections.server.models;

import com.connections.common.models.Category;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a puzzle consisting of 16 words in 4 groups.
 */
public class Puzzle {
    private int gameId;
    private List<Category> groups;

    public Puzzle(int gameId, List<Category> groups) {
        this.gameId = gameId;
        this.groups = groups;
    }

    public int getGameId() {
        return gameId;
    }

    public List<Category> getGroups() {
        return groups;
    }

    public List<String> getAllWords() {
        List<String> allWords = new ArrayList<>();
        for (Category group : groups) {
            allWords.addAll(group.getWords());
        }
        return allWords;
    }
}
