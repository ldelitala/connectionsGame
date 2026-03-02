package com.connections.common.dto;

import com.connections.common.WordGroup;
import java.util.List;

public class CurrentGameStatusDTO extends GameStatusDTO {
    private final long remainingTime;
    private final List<WordGroup> foundGroups;
    private final List<String> remainingWords;

    public CurrentGameStatusDTO(int gameId, int mistakes, int score, boolean finished, 
                               long remainingTime, List<WordGroup> foundGroups, List<String> remainingWords) {
        super(gameId, mistakes, score, finished);
        this.remainingTime = remainingTime;
        this.foundGroups = foundGroups;
        this.remainingWords = remainingWords;
    }

    public long getRemainingTime() { return remainingTime; }
    public List<WordGroup> getFoundGroups() { return foundGroups; }
    public List<String> getRemainingWords() { return remainingWords; }
}
