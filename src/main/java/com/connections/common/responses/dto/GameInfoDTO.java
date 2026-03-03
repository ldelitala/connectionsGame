package com.connections.common.responses.dto;

import com.connections.common.WordGroup;
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
     */
    public static GameInfoDTO current(int gameId, int mistakes, int score, boolean finished,
                                     long remainingTime, List<WordGroup> foundGroups, List<String> remainingWords) {
        return new GameInfoDTO(gameId, mistakes, score, finished, true, remainingTime, foundGroups, remainingWords, null, null);
    }

    /**
     * Factory method for past game info.
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
