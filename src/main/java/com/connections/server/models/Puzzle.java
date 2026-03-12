package com.connections.server.models;

import com.connections.common.models.Category;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a puzzle consisting of 16 words in 4 groups.
 */
public class Puzzle {
    @SerializedName("gameId")
    private int puzzleId;
    private List<Category> groups;

    public Puzzle(int puzzleId, List<Category> groups) {
        this.puzzleId = puzzleId;
        this.groups = groups;
    }

    public int getPuzzleId() {
        return puzzleId;
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
