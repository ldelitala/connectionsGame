package it.unipi.connections.common.protocol.server_responses.dto;

import java.util.List;

/**
 * Data Transfer Object (DTO) representing a leaderboard containing multiple entries. Each entry is
 * represented by a LeaderboardEntryDTO, which includes the player's username their total points and
 * their rank. This DTO is used in responses to operations that request leaderboard information,
 * allowing the server to send a structured list of top players and their scores to clients. The
 * 'entries' field is a list of LeaderboardEntryDTO objects, which can be sorted by points or rank
 * as needed by the server logic before being sent to clients.
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
