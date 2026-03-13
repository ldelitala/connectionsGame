package com.connections.server.logic;

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

    public boolean register(String username, String password) {
        boolean success = usersDAO.register(username, password);
        if (success) historyCoordinator.refreshLeaderboard();
        return success;
    }

    public User login(String username, String password, String host, int udpPort) {
        User user = usersDAO.authenticate(username, password);
        if (user != null) {
            onlineUsers.add(username);
            udpNotifier.registerClient(username, host, udpPort);
            return user;
        }
        return null;
    }

    public void logout(String username) {
        if (username != null) {
            onlineUsers.remove(username);
            udpNotifier.removeClient(username);
        }
    }

    public boolean updateCredentials(String oldName, String oldPsw, String newName, String newPsw) {
        boolean success = usersDAO.updateCredentials(oldName, oldPsw, newName, newPsw);
        if (success) {
            historyCoordinator.refreshLeaderboard();
            if (newName != null && !newName.equals(oldName)) {
                // If username changed, update the online status and notifier
                if (onlineUsers.remove(oldName)) {
                    onlineUsers.add(newName);
                    udpNotifier.removeClient(oldName);
                }
            }
        }
        return success;
    }

    public boolean isOnline(String username) {
        return onlineUsers.contains(username);
    }

    public Set<String> getOnlineUsers() {
        return onlineUsers;
    }
    
    public User getUser(String username) {
        return usersDAO.getUser(username);
    }

    public boolean saveUser(User user) {
        return usersDAO.saveUser(user);
    }
}
