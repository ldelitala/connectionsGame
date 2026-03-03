package com.connections.common.responses.dto;

/**
 * DTO for the result of a word proposal.
 */
public class ProposalResultDTO implements DTO {
    private final String status;
    private final float pointsDelta;
    private final Integer totalPoints;

    private ProposalResultDTO(String status, float pointsDelta, Integer totalPoints) {
        this.status = status;
        this.pointsDelta = pointsDelta;
        this.totalPoints = totalPoints;
    }

    /** Factory for success proposal. */
    public static ProposalResultDTO success(float pointsDelta, Integer totalPoints) {
        return new ProposalResultDTO("success", pointsDelta, totalPoints);
    }

    /** Factory for wrong proposal. */
    public static ProposalResultDTO wrong(float pointsDelta, Integer totalPoints) {
        return new ProposalResultDTO("wrong", pointsDelta, totalPoints);
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
