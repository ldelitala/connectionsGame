package com.connections.client.engine.modules;

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
 * Maintains a persistent network connection to dispatch responses directly to view methods.
 */
public class GameplayModule {

    private final ClientView view;
    private final NetworkGateway gateway;
    private final Gson gson = new Gson();

    public GameplayModule(ClientView view, NetworkGateway gateway) {
        this.view = view;
        this.gateway = gateway;
    }

    /** Submits a guess and displays the puzzle board. */
    public void handleGuess(List<String> words) {
        String jsonReq = gson.toJson(GameplayRequest.submitProposal(words));
        processPuzzleInfo(jsonReq);
    }

    /** Requests and displays the puzzle board. */
    public void handleStatus() {
        String jsonReq = gson.toJson(GameplayRequest.getCurrentInfo());
        processPuzzleInfo(jsonReq);
    }

    /** Requests and displays puzzle global statistics. */
    public void handleStats() {
        try {
            String jsonReq = gson.toJson(GameplayRequest.getCurrentStats());
            String jsonRes = gateway.sendRequest(jsonReq);
            
            Type type = new TypeToken<ServerResponse<PuzzleStatsDTO>>(){}.getType();
            ServerResponse<PuzzleStatsDTO> response = gson.fromJson(jsonRes, type);
            
            view.showResponse(response);
            if (response.getStatus() == Status.OK && response.getData() != null) {
                view.showPuzzleStats(response.getData());
            }
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        }
    }

    private void processPuzzleInfo(String jsonRequest) {
        try {
            String jsonResponse = gateway.sendRequest(jsonRequest);
            Type type = new TypeToken<ServerResponse<PuzzleStateDTO>>(){}.getType();
            ServerResponse<PuzzleStateDTO> response = gson.fromJson(jsonResponse, type);
            
            view.showResponse(response);
            if (response.getStatus() == Status.OK && response.getData() != null) {
                view.showPuzzleInfo(response.getData());
            }
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        }
    }
}
