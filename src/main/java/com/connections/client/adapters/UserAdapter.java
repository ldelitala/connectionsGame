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
 * Handles network logic, JSON parsing, and direct view updates.
 */
public class UserAdapter {

    private final ClientView view;
    private final Gson gson = new Gson();

    public UserAdapter(ClientView view) {
        this.view = view;
    }

    public Status handleRegister(String username, String password, NetworkGateway gateway) {
        ClientRequest request = UserRequest.register(username, password);
        return callAndProcess(request, gateway);
    }

    public Status handleLogin(String username, String password, int udpPort, NetworkGateway gateway) {
        ClientRequest request = UserRequest.login(username, password, udpPort);
        return callAndProcess(request, gateway);
    }

    public Status handleLogout(NetworkGateway gateway) {
        ClientRequest request = UserRequest.logout();
        return callAndProcess(request, gateway);
    }

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
     * Executes a request by converting it to JSON, sending it through the gateway,
     * and displaying the response status/message.
     */
    private Status callAndProcess(ClientRequest request, NetworkGateway gateway) {
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
