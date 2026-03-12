package com.connections.common.protocol.client_requests;

/**
 * specialized Request subclass for user management operations. Handles registration, login, logout,
 * and credential updates.
 */
public class UserRequest extends ClientRequest {

    // register uses "name", login uses "username".
    // gson will serialize based on the factory method's settings.
    private String name;
    private String username;
    private String psw;

    // updateCredentials fields
    private String oldName;
    private String newName;
    private String oldPsw;
    private String newPsw;

    private Integer udpPort;

    private UserRequest(Operation operation) {
        super(operation);
    }

    /**
     * Creates a registration request.
     */
    public static UserRequest register(String username, String password) {
        UserRequest req = new UserRequest(Operation.REGISTER);
        req.name = username;
        req.psw = password;
        return req;
    }

    /**
     * Creates a login request.
     */
    public static UserRequest login(String username, String password, Integer udpPort) {
        UserRequest req = new UserRequest(Operation.LOGIN);
        req.username = username;
        req.psw = password;
        req.udpPort = udpPort;
        return req;
    }

    /**
     * Creates a logout request.
     */
    public static UserRequest logout() {
        return new UserRequest(Operation.LOGOUT);
    }

    /**
     * Creates a request to update user credentials.
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
    public String getUsername() {
        return (username != null) ? username : name;
    }

    public String getPassword() {
        return psw;
    }

    public String getOldName() {
        return oldName;
    }

    public String getNewName() {
        return newName;
    }

    public String getOldPsw() {
        return oldPsw;
    }

    public String getNewPsw() {
        return newPsw;
    }

    public Integer getUdpPort() {
        return udpPort;
    }
}
