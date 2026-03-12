package com.connections.common.protocol.server_responses.dto;

import java.util.List;

/**
 * Data Transfer Object (DTO) representing a leaderboard containing multiple entries.
 */
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
