package com.connections.common.notifications;

import java.util.List;

/**
 * DTO for UDP notifications sent when a new game starts.
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
     * Factory method to create a new game notification.
     * 
     * @param gameId the ID of the new game
     * @param words the 16 words for the game
     * @return a new NewGameNotification
     */
    public static NewGameNotification create(int gameId, List<String> words) {
        return new NewGameNotification(gameId, words);
    }

    public String getType() { return type; }
    public int getGameId() { return gameId; }
    public List<String> getWords() { return words; }
}
