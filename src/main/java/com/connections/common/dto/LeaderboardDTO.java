package com.connections.common.dto;

import java.util.List;

public class LeaderboardDTO {
    private final List<LeaderboardEntryDTO> entries;

    public LeaderboardDTO(List<LeaderboardEntryDTO> entries) {
        this.entries = entries;
    }

    public List<LeaderboardEntryDTO> getEntries() {
        return entries;
    }
}
