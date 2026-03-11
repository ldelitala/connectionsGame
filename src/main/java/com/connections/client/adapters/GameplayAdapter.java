package com.connections.client.adapters;

import com.connections.client.network.NetworkGateway;
import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Coordinator for active puzzle gameplay actions.
 * Manages request creation, raw JSON parsing for specific DTOs, and view updates.
 */
public class GameplayAdapter {

    private final ClientView view;
    private final Gson gson = new Gson();

    public GameplayAdapter(ClientView view) {
        this.view = view;
    }

    /**
     * Submits a guess (proposal) and updates the board view.
     */
    public void handleGuess(List<String> words, NetworkGateway gateway) {
        ClientRequest request = GameplayRequest.submitProposal(words);
        callAndProcess(request, gateway, new TypeToken<ServerResponse<PuzzleStateDTO>>(){}.getType());
    }

    /**
     * Requests the current puzzle state and renders the board.
     */
    public void handleStatus(NetworkGateway gateway) {
        ClientRequest request = GameplayRequest.getCurrentInfo();
        callAndProcess(request, gateway, new TypeToken<ServerResponse<PuzzleStateDTO>>(){}.getType());
    }

    /**
     * Requests current puzzle global statistics.
     */
    public void handleStats(NetworkGateway gateway) {
        ClientRequest request = GameplayRequest.getCurrentStats();
        callAndProcess(request, gateway, new TypeToken<ServerResponse<PuzzleStatsDTO>>(){}.getType());
    }

    /**
     * Internal helper to execute a request, parse the typed response, and update the view.
     */
    private <T extends DTO> void callAndProcess(ClientRequest request, NetworkGateway gateway, Type type) {
        try {
            String json = gateway.sendRequest(request);
            ServerResponse<T> response = gson.fromJson(json, type);
            
            view.showResponse(response);
            if (response.getStatus() == Status.OK && response.getData() != null) {
                T data = response.getData();
                if (data instanceof PuzzleStateDTO) view.showGameInfo((PuzzleStateDTO) data);
                else if (data instanceof PuzzleStatsDTO) view.showGameStats((PuzzleStatsDTO) data);
            }
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        }
    }
}
