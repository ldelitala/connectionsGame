package com.connections.common;

import com.google.gson.annotations.SerializedName;

/**
 * specialized Request subclass for user management operations.
 * <p>
 * This class handles registration, login, logout, and credential updates.
 * It uses private constructors and static factory methods to ensure requests 
 * are created with the correct parameters for each operation.
 * </p>
 */
public class UserRequest extends Request {

    @SerializedName(value = "name", alternate = {"username"})
    private String username;

    @SerializedName(value = "psw", alternate = {"password"})
    private String password;

    private String oldName;
    private String newName;
    private String oldPsw;
    private String newPsw;
    private Integer udpPort;

    /**
     * Private constructor to force use of static factory methods.
     * 
     * @param operation the specific user operation
     */
    private UserRequest(Operation operation) {
        super(operation);
    }

    // --- FACTORY METHODS ---

    /**
     * Creates a registration request.
     * 
     * @param username the username to register
     * @param password the password for the new account
     * @return a new UserRequest for registration
     */
    public static UserRequest register(String username, String password) {
        UserRequest req = new UserRequest(Operation.REGISTER);
        req.username = username;
        req.password = password;
        return req;
    }

    /**
     * Creates a login request.
     * 
     * @param username the user's username
     * @param password the user's password
     * @param udpPort the port where the client listens for UDP notifications
     * @return a new UserRequest for login
     */
    public static UserRequest login(String username, String password, Integer udpPort) {
        UserRequest req = new UserRequest(Operation.LOGIN);
        req.username = username;
        req.password = password;
        req.udpPort = udpPort;
        return req;
    }

    /**
     * Creates a logout request.
     * 
     * @return a new UserRequest for logout
     */
    public static UserRequest logout() {
        return new UserRequest(Operation.LOGOUT);
    }

    /**
     * Creates a request to update user credentials.
     * 
     * @param oldName the current username
     * @param oldPsw the current password
     * @param newName the new username (null to keep current)
     * @param newPsw the new password (null to keep current)
     * @return a new UserRequest for updating credentials
     */
    public static UserRequest updateCredentials(String oldName, String oldPsw, String newName,
            String newPsw) {
        UserRequest req = new UserRequest(Operation.UPDATE_CREDENTIALS);
        req.oldName = oldName;
        req.oldPsw = oldPsw;
        req.newName = newName;
        req.newPsw = newPsw;
        return req;
    }

    // --- GETTERS ---
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getOldName() { return oldName; }
    public String getNewName() { return newName; }
    public String getOldPsw() { return oldPsw; }
    public String getNewPsw() { return newPsw; }
    public Integer getUdpPort() { return udpPort; }
}
