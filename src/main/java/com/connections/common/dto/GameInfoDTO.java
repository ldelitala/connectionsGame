package com.connections.common.dto;

public abstract class GameInfoDTO {
    private final int gameId;
    private final int mistakes;
    private final int score;
    private final boolean finished;

    public GameInfoDTO(int gameId, int mistakes, int score, boolean finished) {
        this.gameId = gameId;
        this.mistakes = mistakes;
        this.score = score;
        this.finished = finished;
    }

    public int getGameId() { return gameId; }
    public int getMistakes() { return mistakes; }
    public int getScore() { return score; }
    public boolean isFinished() { return finished; }
}
