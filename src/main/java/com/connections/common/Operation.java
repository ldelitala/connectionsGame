package com.connections.common;

/**
 * Enum representing all possible operations the client can request.
 */
public enum Operation {
    // User Management
    REGISTER("register"), LOGIN("login"), LOGOUT("logout"), UPDATE_CREDENTIALS("updateCredentials"),

    // Game Management
    SUBMIT_PROPOSAL("submitProposal"), REQUEST_GAME_INFO("requestGameInfo"), REQUEST_GAME_STATS(
            "requestGameStats"),

    // Information Requests
    REQUEST_PLAYER_STATS("requestPlayerStats"), REQUEST_LEADERBOARD("requestLeaderboard");

    private final String value;

    Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Finds an Operation enum by its string value.
     */
    public static Operation fromString(String text) {
        for (Operation op : Operation.values()) {
            if (op.value.equalsIgnoreCase(text)) {
                return op;
            }
        }
        return null;
    }
}
