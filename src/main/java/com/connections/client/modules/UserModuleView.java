package com.connections.client.modules;

import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.UserRequest;
import com.connections.common.protocol.server_responses.ServerResponse;

/**
 * Handles account-related request creation and response display.
 */
public class UserModuleView implements ModuleView {

    // --- Placeholders ---
    private static final String NO_CHANGE = "-";

    // --- Indices ---
    private static final int INDEX_USERNAME = 1;
    private static final int INDEX_PASSWORD = 2;
    private static final int INDEX_OLD_PASSWORD = 1;
    private static final int INDEX_NEW_USERNAME = 2;
    private static final int INDEX_NEW_PASSWORD = 3;
    
    private static final int MIN_TOKENS_AUTH = 3;
    private static final int MIN_TOKENS_UPDATE = 4;

    private final int udpPort;

    public UserModuleView(int udpPort) {
        this.udpPort = udpPort;
    }

    public ClientRequest prepareRegister(String[] tokens) {
        String user, pass;
        if (tokens.length >= MIN_TOKENS_AUTH) {
            user = tokens[INDEX_USERNAME];
            pass = tokens[INDEX_PASSWORD];
        } else {
            System.out.println("Usage: register <username> <password>");
            return null;
        }
        return UserRequest.register(user, pass);
    }

    public ClientRequest prepareLogin(String[] tokens) {
        String user, pass;
        if (tokens.length >= MIN_TOKENS_AUTH) {
            user = tokens[INDEX_USERNAME];
            pass = tokens[INDEX_PASSWORD];
        } else {
            System.out.println("Usage: login <username> <password>");
            return null;
        }
        return UserRequest.login(user, pass, udpPort);
    }

    public ClientRequest prepareLogout() {
        return UserRequest.logout();
    }

    public ClientRequest prepareUpdate(String currentUsername, String[] tokens) {
        if (tokens.length < MIN_TOKENS_UPDATE) {
            System.out.println("Usage: update <oldPassword> <newUsername> <newPassword>");
            System.out.println("Use '" + NO_CHANGE + "' to keep a value unchanged.");
            return null;
        }

        String oldPassword = tokens[INDEX_OLD_PASSWORD];
        String newUsername = tokens[INDEX_NEW_USERNAME];
        String newPassword = tokens[INDEX_NEW_PASSWORD];

        if (newUsername.equals(NO_CHANGE)) {
            newUsername = null;
        }
        if (newPassword.equals(NO_CHANGE)) {
            newPassword = null;
        }

        if (newUsername == null && newPassword == null) {
            System.out.println("No changes provided.");
            return null;
        }

        // The protocol requires oldName (currentUsername) and oldPsw to authorize the change.
        return UserRequest.updateCredentials(currentUsername, oldPassword, newUsername, newPassword);
    }

    @Override
    public void displayResponse(ServerResponse<?> response) {
        System.out.println("[" + response.getStatus() + "] " + response.getMessage());
    }
}
