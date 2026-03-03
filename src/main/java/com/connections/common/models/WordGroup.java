package com.connections.common.models;

import java.util.List;

/**
 * Represents a group of 4 words with a common theme.
 */
public class WordGroup {
    private String theme;
    private List<String> words;

    public WordGroup(String theme, List<String> words) {
        if (words == null || words.size() != 4) {
            throw new IllegalArgumentException("A word group must contain exactly 4 words.");
        }
        this.theme = theme;
        this.words = words;
    }

    public String getTheme() {
        return theme;
    }

    public List<String> getWords() {
        return words;
    }

    @Override
    public String toString() {
        return theme + ": " + words;
    }
}
