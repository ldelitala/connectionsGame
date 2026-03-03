package com.connections.common.responses.dto;

import com.connections.common.models.WordGroup;
import java.util.List;

/**
 * DTO for personal game information (both current and past).
 */
public class GameInfoDTO implements DTO {
    // Common fields
    private final int gameId;
    private final int mistakes;
    private final int score;
    private final boolean finished;
    private final boolean isCurrent;

    // Current game fields
    private final Long remainingTime;
    private final List<WordGroup> foundGroups;
    private final List<String> remainingWords;

    // Past game fields
    private final Boolean won;
    private final List<WordGroup> allGroups;

    private GameInfoDTO(int gameId, int mistakes, int score, boolean finished, boolean isCurrent,
                       Long remainingTime, List<WordGroup> foundGroups, List<String> remainingWords,
                       Boolean won, List<WordGroup> allGroups) {
        if (mistakes < 0) {
            throw new IllegalArgumentException("Mistakes cannot be negative.");
        }
        this.gameId = gameId;
        this.mistakes = mistakes;
        this.score = score;
        this.finished = finished;
        this.isCurrent = isCurrent;
        this.remainingTime = remainingTime;
        this.foundGroups = foundGroups;
        this.remainingWords = remainingWords;
        this.won = won;
        this.allGroups = allGroups;
    }

    /**
     * Factory method for current game info.
     * 
     * @param gameId current game ID
     * @param mistakes current number of mistakes
     * @param score current player score
     * @param finished whether the game is considered finished by the server
     * @param won whether the game has been won
     * @param remainingTime time left in milliseconds
     * @param foundGroups list of groups already found
     * @param remainingWords list of words not yet grouped
     * @return a new GameInfoDTO for the current session
     */
    public static GameInfoDTO current(int gameId, int mistakes, int score, boolean finished, boolean won,
                                     long remainingTime, List<WordGroup> foundGroups, List<String> remainingWords) {
        return new GameInfoDTO(gameId, mistakes, score, finished, true, remainingTime, foundGroups, remainingWords, won, null);
    }

    /**
     * Factory method for past game info.
     * 
     * @param gameId past game ID
     * @param mistakes final mistakes
     * @param score final score
     * @param finished whether the game was completed
     * @param won whether the game was won
     * @param allGroups all word groups in that game
     * @return a new GameInfoDTO for a historical game
     */
    public static GameInfoDTO past(int gameId, int mistakes, int score, boolean finished, boolean won, List<WordGroup> allGroups) {
        return new GameInfoDTO(gameId, mistakes, score, finished, false, null, null, null, won, allGroups);
    }

    // --- GETTERS ---
    public int getGameId() { return gameId; }
    public int getMistakes() { return mistakes; }
    public int getScore() { return score; }
    public boolean isFinished() { return finished; }
    public boolean isCurrent() { return isCurrent; }
    public Long getRemainingTime() { return remainingTime; }
    public List<WordGroup> getFoundGroups() { return foundGroups; }
    public List<String> getRemainingWords() { return remainingWords; }
    public Boolean isWon() { return won; }
    public List<WordGroup> getAllGroups() { return allGroups; }
}
