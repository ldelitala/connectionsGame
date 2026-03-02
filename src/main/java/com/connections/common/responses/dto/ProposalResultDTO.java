package com.connections.common.responses.dto;

/**
 * DTO for the result of a word proposal.
 */
public class ProposalResultDTO {
    private final String status;
    private final String message;
    private final Integer foundGroups;
    private final Integer mistakes;
    private final Boolean gameFinished;
    private final Integer finalPoints;

    private ProposalResultDTO(String status, String message, Integer foundGroups, Integer mistakes, 
                            Boolean gameFinished, Integer finalPoints) {
        this.status = status;
        this.message = message;
        this.foundGroups = foundGroups;
        this.mistakes = mistakes;
        this.gameFinished = gameFinished;
        this.finalPoints = finalPoints;
    }

    /** Factory for success proposal. */
    public static ProposalResultDTO ok(String msg, int found, boolean finished, Integer points) {
        return new ProposalResultDTO("ok", msg, found, null, finished, points);
    }

    /** Factory for wrong proposal. */
    public static ProposalResultDTO wrong(String msg, int mistakes, boolean finished, Integer points) {
        return new ProposalResultDTO("wrong", msg, null, mistakes, finished, points);
    }

    /** Factory for error response. */
    public static ProposalResultDTO error(String msg) {
        return new ProposalResultDTO("error", msg, null, null, null, null);
    }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Integer getFoundGroups() { return foundGroups; }
    public Integer getMistakes() { return mistakes; }
    public Boolean getGameFinished() { return gameFinished; }
    public Integer getFinalPoints() { return finalPoints; }
}
