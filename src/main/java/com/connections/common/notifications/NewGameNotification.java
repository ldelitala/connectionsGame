package com.connections.common.notifications;

import java.util.List;

/**
 * DTO for UDP notifications sent when a new puzzle starts.
 */
public class NewGameNotification implements Notification {
    private final String type = "new_game";
    private final int gameId;
    private final List<String> words;

    private NewGameNotification(int gameId, List<String> words) {
        this.gameId = gameId;
        this.words = words;
    }

    /**
     * Factory method to create a new puzzle notification.
     * 
     * @param gameId the ID of the new puzzle
     * @param words the 16 words for the puzzle
     * @return a new NewGameNotification
     */
    public static NewGameNotification create(int gameId, List<String> words) {
        return new NewGameNotification(gameId, words);
    }

    public String getType() { return type; }
    public int getGameId() { return gameId; }
    public List<String> getWords() { return words; }
}
