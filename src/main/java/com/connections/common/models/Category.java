package com.connections.common.models;

import java.util.List;

/**
 * Represents a group of words with a common theme.
 */
public class Category {

    /** The common theme that connects the words. */
    private String theme;
    /** The list of words belonging to this group. */
    private List<String> words;

    /**
     * Constructs a Category with a theme and a list of words.
     * 
     * @param theme the theme of the group
     * @param words the list of words
     * @throws IllegalArgumentException if words is null or empty
     */
    public Category(String theme, List<String> words) {
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("A word group must contain at least one word.");
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
     * @return the list of words
     */
    public List<String> getWords() {
        return words;
    }

    @Override
    public String toString() {
        return theme + ": " + words;
    }
}
