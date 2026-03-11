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
 * Uses a persistent NetworkGateway to handle identity actions.
 */
public class UserAdapter {

    private final ClientView view;
    private final NetworkGateway gateway;
    private final Gson gson = new Gson();

    public UserAdapter(ClientView view, NetworkGateway gateway) {
        this.view = view;
        this.gateway = gateway;
    }

    public Status handleRegister(String username, String password) {
        ClientRequest request = UserRequest.register(username, password);
        return callAndProcess(request);
    }

    public Status handleLogin(String username, String password, int udpPort) {
        ClientRequest request = UserRequest.login(username, password, udpPort);
        return callAndProcess(request);
    }

    public Status handleLogout() {
        ClientRequest request = UserRequest.logout();
        return callAndProcess(request);
    }

    public Status handleUpdate(String username, String oldPass, String newName, String newPass) {
        String finalName = ("-".equals(newName)) ? null : newName;
        String finalPass = ("-".equals(newPass)) ? null : newPass;

        if (finalName == null && finalPass == null) {
            view.showMessage("No changes provided.");
            return Status.ERROR;
        }

        ClientRequest request = UserRequest.updateCredentials(username, oldPass, finalName, finalPass);
        return callAndProcess(request);
    }

    private Status callAndProcess(ClientRequest request) {
        try {
            String requestJson = gson.toJson(request);
            String responseJson = gateway.sendRequest(requestJson);
            
            Type type = new TypeToken<ServerResponse<DTO>>(){}.getType();
            ServerResponse<DTO> response = gson.fromJson(responseJson, type);
            
            view.showResponse(response);
            return response.getStatus();
            
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
            return Status.ERROR;
        }
    }
}
