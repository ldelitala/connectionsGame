package com.connections.common.dto;

public class ProposalResultDTO {
    private final String status;
    private final String message;
    private final Integer foundGroups;
    private final Integer mistakes;
    private final Boolean gameFinished;
    private final Integer finalPoints;

    public ProposalResultDTO(String status, String message, Integer foundGroups, Integer mistakes, 
                            Boolean gameFinished, Integer finalPoints) {
        this.status = status;
        this.message = message;
        this.foundGroups = foundGroups;
        this.mistakes = mistakes;
        this.gameFinished = gameFinished;
        this.finalPoints = finalPoints;
    }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Integer getFoundGroups() { return foundGroups; }
    public Integer getMistakes() { return mistakes; }
    public Boolean getGameFinished() { return gameFinished; }
    public Integer getFinalPoints() { return finalPoints; }
}
