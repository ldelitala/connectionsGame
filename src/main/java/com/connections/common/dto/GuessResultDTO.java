package com.connections.common.dto;

import java.util.List;

/**
 * DTO for the result of a word proposal.
 */
public class GuessResultDTO implements DTO {
    private final String status;
    private final float pointsDelta;
    private final Integer totalPoints;
    private final List<String> remainingWords;

    private GuessResultDTO(String status, float pointsDelta, Integer totalPoints, List<String> remainingWords) {
        this.status = status;
        this.pointsDelta = pointsDelta;
        this.totalPoints = totalPoints;
        this.remainingWords = remainingWords;
    }

    /** Factory for success proposal. */
    public static GuessResultDTO success(float pointsDelta, Integer totalPoints, List<String> remainingWords) {
        return new GuessResultDTO("success", pointsDelta, totalPoints, remainingWords);
    }

    /** Factory for wrong proposal. */
    public static GuessResultDTO wrong(float pointsDelta, Integer totalPoints, List<String> remainingWords) {
        return new GuessResultDTO("wrong", pointsDelta, totalPoints, remainingWords);
    }

    public String getStatus() {
        return status;
    }

    public float getPointsDelta() {
        return pointsDelta;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public List<String> getRemainingWords() {
        return remainingWords;
    }
}
