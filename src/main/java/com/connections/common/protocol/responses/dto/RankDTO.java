package com.connections.common.protocol.responses.dto;

/**
 * DTO for a single rank value.
 */
public class RankDTO implements DTO {
    private final int rank;

    private RankDTO(int rank) {
        this.rank = rank;
    }

    public static RankDTO create(int rank) {
        return new RankDTO(rank);
    }

    public int getRank() {
        return rank;
    }
}
