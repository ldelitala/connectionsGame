package it.unipi.connections.common.models;

import java.util.List;

/**
 * Represents a group of words with a common theme.
 */
public class Category {

    /** The common theme that connects the words. */
    private String theme;
    /** The list of words belonging to this group. */
    private List<String> words;

    public Category(String theme, List<String> words) {
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("A word group must contain at least one word.");
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
