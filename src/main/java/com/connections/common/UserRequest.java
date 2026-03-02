package com.connections.common;

import com.google.gson.annotations.SerializedName;

/**
 * Implementation for User Management operations (register, login, logout, updateCredentials).
 */
public class UserRequest extends Request {
    private final String username;
    private final String password;
    
    // For updateCredentials
    private final String newName;
    private final String oldPsw;
    private final String newPsw;
    
    // For login
    private final Integer udpPort;

    public UserRequest(Operation op, String username, String password, String newName, String oldPsw, String newPsw, Integer udpPort) {
        super(op);
        this.username = username;
        this.password = password;
        this.newName = newName;
        this.oldPsw = oldPsw;
        this.newPsw = newPsw;
        this.udpPort = udpPort;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNewName() { return newName; }
    public String getOldPsw() { return oldPsw; }
    public String getNewPsw() { return newPsw; }
    public Integer getUdpPort() { return udpPort; }
}
