package com.connections.common.responses.dto;

import java.util.List;

/**
 * DTO for UDP notifications sent when a new game starts.
 */
public class NewGameNotificationDTO {
    private final String type = "new_game";
    private final int gameId;
    private final List<String> words;

    private NewGameNotificationDTO(int gameId, List<String> words) {
        this.gameId = gameId;
        this.words = words;
    }

    /**
     * Factory method to create a new game notification.
     * 
     * @param gameId the ID of the new game
     * @param words the 16 words for the game
     * @return a new NewGameNotificationDTO
     */
    public static NewGameNotificationDTO create(int gameId, List<String> words) {
        return new NewGameNotificationDTO(gameId, words);
    }

    public String getType() { return type; }
    public int getGameId() { return gameId; }
    public List<String> getWords() { return words; }
}
