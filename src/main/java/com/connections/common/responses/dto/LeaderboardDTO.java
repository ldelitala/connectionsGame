package com.connections.common.responses.dto;

import java.util.List;

public class LeaderboardDTO implements DTO {
    private final List<LeaderboardEntryDTO> entries;

    private LeaderboardDTO(List<LeaderboardEntryDTO> entries) {
        this.entries = entries;
    }

    public static LeaderboardDTO create(List<LeaderboardEntryDTO> entries) {
        return new LeaderboardDTO(entries);
    }

    public List<LeaderboardEntryDTO> getEntries() {
        return entries;
    }
}
