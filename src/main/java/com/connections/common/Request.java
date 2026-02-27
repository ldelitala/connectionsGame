package com.connections.common;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * Standard JSON request format from client to server.
 */
public class Request {
    private String operation;
    
    @SerializedName(value = "username", alternate = {"name"})
    private String username;
    
    @SerializedName(value = "psw", alternate = {"password"})
    private String password;
    
    @SerializedName("oldName")
    private String oldUsername;
    
    @SerializedName("newName")
    private String newUsername;
    
    @SerializedName("oldPsw")
    private String oldPassword;
    
    @SerializedName("newPsw")
    private String newPassword;
    
    private List<String> words;
    private Integer gameId;
    
    @SerializedName("playerName")
    private String playerName;
    
    @SerializedName("topPlayers")
    private Integer topPlayers;
    
    private Integer udpPort;

    public Request() {}

    public Request(String operation) {
        this.operation = operation;
    }

    // Getters and Setters
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getOldUsername() { return oldUsername; }
    public void setOldUsername(String oldUsername) { this.oldUsername = oldUsername; }
    public String getNewUsername() { return newUsername; }
    public void setNewUsername(String newUsername) { this.newUsername = newUsername; }
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public List<String> getWords() { return words; }
    public void setWords(List<String> words) { this.words = words; }
    public Integer getGameId() { return gameId; }
    public void setGameId(Integer gameId) { this.gameId = gameId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public Integer getTopPlayers() { return topPlayers; }
    public void setTopPlayers(Integer topPlayers) { this.topPlayers = topPlayers; }
    public Integer getUdpPort() { return udpPort; }
    public void setUdpPort(Integer udpPort) { this.udpPort = udpPort; }
}
