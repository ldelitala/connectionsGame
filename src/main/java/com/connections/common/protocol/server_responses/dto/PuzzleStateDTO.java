package com.connections.common.protocol.server_responses.dto;

import com.connections.common.models.Category;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO for personal puzzle information (both current and past). Follows the project's specific
 * requirements for what info to return for active vs finished games. Uses factory methods to create
 * instances for active (in progress) vs concluded (past) puzzles, ensuring that only relevant
 * fields are populated for each case.
 */
public class PuzzleStateDTO implements DTO {
    @SerializedName("gameId")
    private final int puzzleId;
    private final boolean finished;
    private final int mistakes;
    private final int score;

    // Fields used in ACTIVE puzzle
    private final Long remainingTime;
    private final List<String> foundGroups;
    private final List<String> remainingWords;

    // Fields used in FINISHED puzzle
    private final List<Category> allGroups; // All 16 words assigned to 4 groups
    private final Integer correctGroupsCount;

    private PuzzleStateDTO(int puzzleId, boolean finished, int mistakes, int score,
            Long remainingTime, List<String> foundGroups, List<String> remainingWords,
            List<Category> allGroups, Integer correctGroupsCount) {

        this.puzzleId = puzzleId;
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
     * Factory method for active (in progress) puzzle info.
     */
    public static PuzzleStateDTO active(int puzzleId, long remainingTime, List<String> foundGroups,
            List<String> remainingWords, int mistakes, int score) {
        return new PuzzleStateDTO(puzzleId, false, mistakes, score, remainingTime, foundGroups,
                remainingWords, null, null);
    }

    /**
     * Factory method for concluded (past) puzzle info.
     * 
     * @param puzzleId past puzzle ID
     * @param allGroups all 16 words assigned to their 4 groups
     * @param correctGroupsCount number of groups correctly guessed
     * @param mistakes final mistakes
     * @param score final score
     * @return a new PuzzleStateDTO for a historical puzzle
     */
    public static PuzzleStateDTO concluded(int puzzleId, List<Category> allGroups,
            int correctGroupsCount, int mistakes, int score) {
        return new PuzzleStateDTO(puzzleId, true, mistakes, score, null, null, null, allGroups,
                correctGroupsCount);
    }

    // --- GETTERS ---
    public int getPuzzleId() {
        return puzzleId;
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

    public List<String> getFoundGroups() {
        return foundGroups;
    }

    public List<String> getRemainingWords() {
        return remainingWords;
    }

    public List<Category> getAllGroups() {
        return allGroups;
    }

    public Integer getCorrectGroupsCount() {
        return correctGroupsCount;
    }
}
