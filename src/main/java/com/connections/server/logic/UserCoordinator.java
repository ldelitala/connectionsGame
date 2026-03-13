package com.connections.server.logic;

import com.connections.common.protocol.Operation;
import com.connections.common.protocol.client_requests.UserRequest;
import com.connections.common.protocol.server_responses.UserResponse;
import com.connections.server.network.UdpNotifier;
import com.connections.server.models.User;
import com.connections.server.persistence.UsersDAO;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinator for User-related business logic.
 * Manages active sessions and bridges between persistence and real-time notifications.
 */
public class UserCoordinator {
    private final UsersDAO usersDAO;
    private final UdpNotifier udpNotifier;
    private final HistoryCoordinator historyCoordinator;
    
    // Set of usernames currently logged in
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public UserCoordinator(UsersDAO usersDAO, UdpNotifier udpNotifier, HistoryCoordinator historyCoordinator) {
        this.usersDAO = usersDAO;
        this.udpNotifier = udpNotifier;
        this.historyCoordinator = historyCoordinator;
    }

    /**
     * Centralized processing for user-related requests.
     * 
     * @param req the user request
     * @param currentUsername the username currently associated with the session (if any)
     * @return a specialized UserResponse
     */
    public UserResponse handle(UserRequest req, String currentUsername) {
        Operation op = req.getOperation();
        switch (op) {
            case REGISTER:
                return register(req.getUsername(), req.getPassword()) 
                    ? UserResponse.ok(op, "Registered successfully.") 
                    : UserResponse.error(op, "Username already exists.");

            case LOGIN:
                User user = login(req.getUsername(), req.getPassword(), "localhost", req.getUdpPort());
                if (user != null) {
                    return UserResponse.ok(op, "Login successful.");
                }
                return UserResponse.error(op, "Invalid credentials.");

            case LOGOUT:
                if (currentUsername != null) {
                    logout(currentUsername);
                    return UserResponse.ok(op, "Logout successful.");
                }
                return UserResponse.error(op, "Not logged in.");

            case UPDATE_CREDENTIALS:
                if (currentUsername == null) return UserResponse.forbidden(op, "Login required.");
                boolean success = updateCredentials(currentUsername, req.getOldPsw(), 
                                                   req.getNewName(), req.getNewPsw());
                return success ? UserResponse.ok(op, "Credentials updated.") : UserResponse.error(op, "Update failed.");

            default: return UserResponse.error(op, "Invalid user operation.");
        }
    }

    public synchronized boolean register(String username, String password) {
        return usersDAO.register(username, password);
    }

    public synchronized User login(String username, String password, String host, int udpPort) {
        User u = usersDAO.authenticate(username, password);
        if (u != null) {
            onlineUsers.add(username);
            udpNotifier.registerClient(username, host, udpPort);
            return u;
        }
        return null;
    }

    public synchronized void logout(String username) {
        if (username != null) {
            onlineUsers.remove(username);
            udpNotifier.unregisterClient(username);
        }
    }

    public synchronized boolean updateCredentials(String oldName, String oldPsw, String newName, String newPsw) {
        boolean success = usersDAO.updateCredentials(oldName, oldPsw, newName, newPsw);
        if (success && newName != null && !newName.equals(oldName)) {
            if (onlineUsers.remove(oldName)) {
                onlineUsers.add(newName);
            }
            udpNotifier.updateClientKey(oldName, newName);
            historyCoordinator.renameUser(oldName, newName);
        }
        return success;
    }

    public User getUser(String username) {
        return usersDAO.getUser(username);
    }

    public boolean saveUser(User user) {
        return usersDAO.saveUser(user);
    }
}
