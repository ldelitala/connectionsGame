package com.connections.common.dto;

import com.connections.common.models.WordGroup;
import java.util.List;

/**
 * DTO for personal game information (both current and past).
 * Follows the project's specific requirements for what info to return for active vs finished games.
 */
public class GameInfoDTO implements DTO {
    private final int gameId;
    private final boolean finished;
    private final int mistakes;
    private final int score;

    // Fields used in ACTIVE game
    private final Long remainingTime;
    private final List<WordGroup> foundGroups;
    private final List<String> remainingWords;

    // Fields used in FINISHED game
    private final List<WordGroup> allGroups; // All 16 words assigned to 4 groups
    private final Integer correctGroupsCount;

    private GameInfoDTO(int gameId, boolean finished, int mistakes, int score,
            Long remainingTime, List<WordGroup> foundGroups, List<String> remainingWords,
            List<WordGroup> allGroups, Integer correctGroupsCount) {

        this.gameId = gameId;
        this.finished = finished;
        this.mistakes = mistakes;
        this.score = score;
        this.remainingTime = remainingTime;
        this.foundGroups = foundGroups;
        this.remainingWords = remainingWords;
        this.allGroups = allGroups;
        this.correctGroupsCount = correctGroupsCount;
    }

    /**
     * Factory method for active (in progress) game info.
     * 
     * @param gameId game ID
     * @param remainingTime time left in milliseconds
     * @param foundGroups list of groups already found
     * @param remainingWords list of words not yet grouped
     * @param mistakes current number of mistakes
     * @param score current player score
     * @return a new GameInfoDTO for the active session
     */
    public static GameInfoDTO active(int gameId, long remainingTime, List<WordGroup> foundGroups,
            List<String> remainingWords, int mistakes, int score) {
        return new GameInfoDTO(gameId, false, mistakes, score, remainingTime, foundGroups,
                remainingWords, null, null);
    }

    /**
     * Factory method for concluded (past) game info.
     * 
     * @param gameId past game ID
     * @param allGroups all 16 words assigned to their 4 groups
     * @param correctGroupsCount number of groups correctly guessed
     * @param mistakes final mistakes
     * @param score final score
     * @return a new GameInfoDTO for a historical game
     */
    public static GameInfoDTO concluded(int gameId, List<WordGroup> allGroups,
            int correctGroupsCount, int mistakes, int score) {
        return new GameInfoDTO(gameId, true, mistakes, score, null, null, null, allGroups,
                correctGroupsCount);
    }

    // --- GETTERS ---
    public int getGameId() {
        return gameId;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getMistakes() {
        return mistakes;
    }

    public int getScore() {
        return score;
    }

    public Long getRemainingTime() {
        return remainingTime;
    }

    public List<WordGroup> getFoundGroups() {
        return foundGroups;
    }

    public List<String> getRemainingWords() {
        return remainingWords;
    }

    public List<WordGroup> getAllGroups() {
        return allGroups;
    }

    public Integer getCorrectGroupsCount() {
        return correctGroupsCount;
    }
}
