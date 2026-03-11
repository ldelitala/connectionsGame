package com.connections.client.services;

import com.connections.client.ui.ClientView;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.UserRequest;
import com.connections.common.protocol.server_responses.ServerResponse;

/**
 * Handles account-related operations like registration, login, and updates.
 */
public class IdentityService {

    private final ClientView view;

    public IdentityService(ClientView view) {
        this.view = view;
    }

    /** Prepares a registration request. */
    public ClientRequest prepareRegister(String username, String password) {
        return UserRequest.register(username, password);
    }

    /** Prepares a login request. */
    public ClientRequest prepareLogin(String username, String password, int udpPort) {
        return UserRequest.login(username, password, udpPort);
    }

    /** Prepares a logout request. */
    public ClientRequest prepareLogout() {
        return UserRequest.logout();
    }

    /** Prepares a credential update request. */
    public ClientRequest prepareUpdate(String oldName, String oldPass, String newName, String newPass) {
        // Normalize placeholders if provided via UI
        String finalName = ("-".equals(newName)) ? null : newName;
        String finalPass = ("-".equals(newPass)) ? null : newPass;

        if (finalName == null && finalPass == null) {
            view.showMessage("No changes provided.");
            return null;
        }

        return UserRequest.updateCredentials(oldName, oldPass, finalName, finalPass);
    }

    /** Displays the server response for identity operations. */
    public void handleResponse(ServerResponse<?> response) {
        view.showResponse(response);
    }
}
