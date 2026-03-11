package com.connections.client.services;

import com.connections.client.ui.ClientView;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.UserRequest;
import com.connections.common.protocol.server_responses.ServerResponse;

/**
 * Handles account-related request creation and response display.
 */
public class IdentityService {

    private static final String NO_CHANGE = "-";
    private static final int INDEX_USERNAME = 1;
    private static final int INDEX_PASSWORD = 2;
    private static final int INDEX_OLD_PASSWORD = 1;
    private static final int INDEX_NEW_USERNAME = 2;
    private static final int INDEX_NEW_PASSWORD = 3;

    private static final int MIN_TOKENS_AUTH = 3;
    private static final int MIN_TOKENS_UPDATE = 4;

    private final ClientView view;

    public IdentityService(ClientView view) {
        this.view = view;
    }

    public ClientRequest prepareRegister(String[] tokens) {
        if (tokens.length < MIN_TOKENS_AUTH) {
            view.showMessage("Usage: register <username> <password>");
            return null;
        }
        return UserRequest.register(tokens[INDEX_USERNAME], tokens[INDEX_PASSWORD]);
    }

    public ClientRequest prepareLogin(String[] tokens, int udpPort) {
        if (tokens.length < MIN_TOKENS_AUTH) {
            view.showMessage("Usage: login <username> <password>");
            return null;
        }
        return UserRequest.login(tokens[INDEX_USERNAME], tokens[INDEX_PASSWORD], udpPort);
    }

    public ClientRequest prepareLogout() {
        return UserRequest.logout();
    }

    public ClientRequest prepareUpdate(String currentUsername, String[] tokens) {
        if (tokens.length < MIN_TOKENS_UPDATE) {
            view.showMessage("Usage: update <oldPassword> <newUsername> <newPassword>\n" +
                             "Use '" + NO_CHANGE + "' to keep a value unchanged.");
            return null;
        }

        String oldPassword = tokens[INDEX_OLD_PASSWORD];
        String newUsername = tokens[INDEX_NEW_USERNAME];
        String newPassword = tokens[INDEX_NEW_PASSWORD];

        if (NO_CHANGE.equals(newUsername)) newUsername = null;
        if (NO_CHANGE.equals(newPassword)) newPassword = null;

        if (newUsername == null && newPassword == null) {
            view.showMessage("No changes provided.");
            return null;
        }

        return UserRequest.updateCredentials(currentUsername, oldPassword, newUsername, newPassword);
    }

    public void handleResponse(ServerResponse<?> response) {
        view.showResponse(response);
    }
}
