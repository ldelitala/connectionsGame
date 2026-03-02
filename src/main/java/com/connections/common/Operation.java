package com.connections.common;

import com.google.gson.annotations.SerializedName;

/**
 * Enum representing all possible operations the client can request from the server.
 * <p>
 * This enum is used to identify the type of request and ensures consistent 
 * communication between the client and the server. It uses Gson's {@code @SerializedName} 
 * to map the enum constants to specific JSON string values.
 * </p>
 */
public enum Operation {
    /** Register a new user account. */
    @SerializedName("register")
    REGISTER,
    
    /** Authenticate a user and start a session. */
    @SerializedName("login")
    LOGIN,
    
    /** Terminate the current user session. */
    @SerializedName("logout")
    LOGOUT,
    
    /** Update the username or password of the current user. */
    @SerializedName("updateCredentials")
    UPDATE_CREDENTIALS,
    
    /** Submit a 4-word grouping proposal for the current game. */
    @SerializedName("submitProposal")
    SUBMIT_PROPOSAL,
    
    /** Request detailed information about a specific game or the current game. */
    @SerializedName("requestGameInfo")
    REQUEST_GAME_INFO,
    
    /** Request global statistics for a specific game. */
    @SerializedName("requestGameStats")
    REQUEST_GAME_STATS,
    
    /** Request personal statistics for the logged-in user. */
    @SerializedName("requestPlayerStats")
    REQUEST_PLAYER_STATS,
    
    /** Request the global leaderboard or a specific player's rank. */
    @SerializedName("requestLeaderboard")
    REQUEST_LEADERBOARD;
}
