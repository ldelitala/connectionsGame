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

    /** Checks if the 4 words form a valid group in this puzzle. */
    public boolean isCorrectGroup(List<String> words) {
        for (Category category : groups) {
            if (category.getWords().containsAll(words) && words.containsAll(category.getWords())) {
                return true;
            }
        }
        return false;
    }

    /** Returns the base points for a correct grouping. */
    public double getPointsForWords(List<String> words) {
        return isCorrectGroup(words) ? 6.0 : 0.0;
    }

    /** Returns the category names for the successfully identified groups. */
    public List<String> getCategoriesForGroups(List<List<String>> submittedGroups) {
        List<String> result = new ArrayList<>();
        for (List<String> group : submittedGroups) {
            for (Category cat : groups) {
                if (cat.getWords().containsAll(group)) {
                    result.add(cat.getTheme());
                }
            }
        }
        return result;
    }
}
