package it.unipi.connections.common.protocol.client_requests;

import it.unipi.connections.common.protocol.Operation;
import java.util.EnumSet;

/**
 * specialized Request subclass for user management operations. Handles
 * registration, login, logout,
 * and credential updates.
 */
public class UserRequest extends ClientRequest {

    private static final EnumSet<Operation> ALLOWED_OPERATIONS = Operation.userOps();

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

    // --------------------------------------------------------------------------
    // Factory Methods
    // --------------------------------------------------------------------------

    public static UserRequest register(String username, String password) {
        UserRequest req = new UserRequest(Operation.REGISTER);
        req.name = username;
        req.psw = password;
        return req;
    }

    public static UserRequest login(String username, String password, Integer udpPort) {
        UserRequest req = new UserRequest(Operation.LOGIN);
        req.username = username;
        req.psw = password;
        req.udpPort = udpPort;
        return req;
    }

    public static UserRequest logout() {
        return new UserRequest(Operation.LOGOUT);
    }

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

    @Override
    public boolean isValid() {
        Operation op = getOperation();
        if (!ALLOWED_OPERATIONS.contains(op)) {
            return false;
        }

        switch (op) {
            case REGISTER:
                return name != null && !name.trim().isEmpty() && psw != null && !psw.trim().isEmpty();
            case LOGIN:
                return username != null && !username.trim().isEmpty() && psw != null && !psw.trim().isEmpty();
            case UPDATE_CREDENTIALS:
                // Mandatory current credentials
                boolean currentValid = oldName != null && !oldName.trim().isEmpty() &&
                        oldPsw != null && !oldPsw.trim().isEmpty();
                // At least one new credential must be provided
                boolean anyNewProvided = (newName != null && !newName.trim().isEmpty()) ||
                        (newPsw != null && !newPsw.trim().isEmpty());
                return currentValid && anyNewProvided;
            case LOGOUT:
                return true;
            default:
                return false;
        }
    }

}
