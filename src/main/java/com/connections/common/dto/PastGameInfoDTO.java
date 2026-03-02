package com.connections.common.dto;

import com.connections.common.WordGroup;
import java.util.List;

public class PastGameInfoDTO extends GameInfoDTO {
    private final boolean won;
    private final List<WordGroup> allGroups;

    public PastGameInfoDTO(int gameId, int mistakes, int score, boolean finished, boolean won, List<WordGroup> allGroups) {
        super(gameId, mistakes, score, finished);
        this.won = won;
        this.allGroups = allGroups;
    }

    public boolean isWon() { return won; }
    public List<WordGroup> getAllGroups() { return allGroups; }
}
