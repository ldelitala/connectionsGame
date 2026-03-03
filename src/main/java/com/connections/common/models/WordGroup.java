package com.connections.common.models;

import java.util.List;

/**
 * Represents a group of 4 words with a common theme.
 */
public class WordGroup {

    private String theme;
    private List<String> words;

    /**
     * Constructs a WordGroup with a theme and exactly 4 words.
     * 
     * @param theme the theme of the group
     * @param words the list of 4 words
     * @throws IllegalArgumentException if words is null or does not contain exactly 4 elements
     */
    public WordGroup(String theme, List<String> words) {
        if (words == null || words.size() != 4) {
            throw new IllegalArgumentException("A word group must contain exactly 4 words.");
        }
        this.theme = theme;
        this.words = words;
    }

    /**
     * Gets the theme of this word group.
     * 
     * @return the theme string
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Gets the list of words in this group.
     * 
     * @return the list of 4 words
     */
    public List<String> getWords() {
        return words;
    }

    @Override
    public String toString() {
        return theme + ": " + words;
    }
}
