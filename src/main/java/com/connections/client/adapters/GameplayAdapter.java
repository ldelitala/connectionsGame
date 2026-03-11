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
 * Converts actions to JSON and dispatches responses directly to specific view methods.
 */
public class GameplayAdapter {

    private final ClientView view;
    private final Gson gson = new Gson();

    public GameplayAdapter(ClientView view) {
        this.view = view;
    }

    /** Submits a guess and displays the puzzle board. */
    public void handleGuess(List<String> words, NetworkGateway gateway) {
        String jsonReq = gson.toJson(GameplayRequest.submitProposal(words));
        processPuzzleState(jsonReq, gateway);
    }

    /** Requests and displays the puzzle board. */
    public void handleStatus(NetworkGateway gateway) {
        String jsonReq = gson.toJson(GameplayRequest.getCurrentInfo());
        processPuzzleState(jsonReq, gateway);
    }

    /** Requests and displays puzzle global statistics. */
    public void handleStats(NetworkGateway gateway) {
        try {
            String jsonReq = gson.toJson(GameplayRequest.getCurrentStats());
            String jsonRes = gateway.sendRequest(jsonReq);
            
            Type type = new TypeToken<ServerResponse<PuzzleStatsDTO>>(){}.getType();
            ServerResponse<PuzzleStatsDTO> response = gson.fromJson(jsonRes, type);
            
            view.showResponse(response);
            if (response.getStatus() == Status.OK && response.getData() != null) {
                view.showGameStats(response.getData());
            }
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        }
    }

    /** Helper for actions returning a PuzzleStateDTO. */
    private void processPuzzleState(String jsonRequest, NetworkGateway gateway) {
        try {
            String jsonResponse = gateway.sendRequest(jsonRequest);
            Type type = new TypeToken<ServerResponse<PuzzleStateDTO>>(){}.getType();
            ServerResponse<PuzzleStateDTO> response = gson.fromJson(jsonResponse, type);
            
            view.showResponse(response);
            if (response.getStatus() == Status.OK && response.getData() != null) {
                view.showGameInfo(response.getData());
            }
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        }
    }
}
