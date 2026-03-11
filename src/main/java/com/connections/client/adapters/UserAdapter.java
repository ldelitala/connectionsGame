package com.connections.client.adapters;

import com.connections.client.network.NetworkGateway;
import com.connections.client.ui.ClientView;
import com.connections.common.dto.DTO;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Coordinator for account-related operations.
 * Handles network logic, JSON parsing, and view updates for identity actions.
 */
public class UserAdapter {

    private final ClientView view;
    private final Gson gson = new Gson();

    public UserAdapter(ClientView view) {
        this.view = view;
    }

    /**
     * Executes a registration request.
     */
    public Status handleRegister(String username, String password, NetworkGateway gateway) {
        ClientRequest request = UserRequest.register(username, password);
        return callAndProcess(request, gateway);
    }

    /**
     * Executes a login request.
     */
    public Status handleLogin(String username, String password, int udpPort, NetworkGateway gateway) {
        ClientRequest request = UserRequest.login(username, password, udpPort);
        return callAndProcess(request, gateway);
    }

    /**
     * Executes a logout request.
     */
    public Status handleLogout(NetworkGateway gateway) {
        ClientRequest request = UserRequest.logout();
        return callAndProcess(request, gateway);
    }

    /**
     * Executes a credential update request.
     */
    public Status handleUpdate(String oldName, String oldPass, String newName, String newPass, NetworkGateway gateway) {
        String finalName = ("-".equals(newName)) ? null : newName;
        String finalPass = ("-".equals(newPass)) ? null : newPass;

        if (finalName == null && finalPass == null) {
            view.showMessage("No changes provided.");
            return Status.ERROR;
        }

        ClientRequest request = UserRequest.updateCredentials(oldName, oldPass, finalName, finalPass);
        return callAndProcess(request, gateway);
    }

    /**
     * Generic helper for identity operations that don't return specific DTO data.
     */
    private Status callAndProcess(ClientRequest request, NetworkGateway gateway) {
        try {
            String json = gateway.sendRequest(request);
            // Define the specific type for parsing to ensure GSON knows what to do
            Type type = new TypeToken<ServerResponse<DTO>>(){}.getType();
            ServerResponse<DTO> response = gson.fromJson(json, type);
            
            view.showResponse(response);
            return response.getStatus();
            
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
            return Status.ERROR;
        }
    }
}
