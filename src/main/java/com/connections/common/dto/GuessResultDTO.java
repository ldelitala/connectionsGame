package com.connections.common.dto;

/**
 * DTO for the result of a word proposal.
 */
public class GuessResultDTO implements DTO {
    private final String status;
    private final float pointsDelta;
    private final Integer totalPoints;

    private GuessResultDTO(String status, float pointsDelta, Integer totalPoints) {
        this.status = status;
        this.pointsDelta = pointsDelta;
        this.totalPoints = totalPoints;
    }

    /** Factory for success proposal. */
    public static GuessResultDTO success(float pointsDelta, Integer totalPoints) {
        return new GuessResultDTO("success", pointsDelta, totalPoints);
    }

    /** Factory for wrong proposal. */
    public static GuessResultDTO wrong(float pointsDelta, Integer totalPoints) {
        return new GuessResultDTO("wrong", pointsDelta, totalPoints);
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
}
