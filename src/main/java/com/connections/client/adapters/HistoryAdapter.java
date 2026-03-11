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

/**
 * Coordinator for history-related operations (archives, stats, leaderboards).
 * Converts actions to JSON and dispatches responses directly to specific view methods.
 */
public class HistoryAdapter {

    private final ClientView view;
    private final Gson gson = new Gson();

    public HistoryAdapter(ClientView view) {
        this.view = view;
    }

    /** Requests past puzzle info and renders the board. */
    public void handlePastInfo(int puzzleId, NetworkGateway gateway) {
        try {
            String jsonReq = gson.toJson(HistoryRequest.getGameInfo(puzzleId));
            String jsonRes = gateway.sendRequest(jsonReq);
            
            Type type = new TypeToken<ServerResponse<PuzzleStateDTO>>(){}.getType();
            ServerResponse<PuzzleStateDTO> response = gson.fromJson(jsonRes, type);
            
            view.showResponse(response);
            if (response.getStatus() == Status.OK && response.getData() != null) {
                view.showGameInfo(response.getData());
            }
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        }
    }

    /** Requests global stats for a past puzzle and renders statistics. */
    public void handleGlobalStats(int puzzleId, NetworkGateway gateway) {
        try {
            String jsonReq = gson.toJson(HistoryRequest.getGameStats(puzzleId));
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

    /** Requests leaderboard or player ranking and renders the leaderboard. */
    public void handleRanking(String username, Integer topK, NetworkGateway gateway) {
        try {
            String jsonReq = gson.toJson(HistoryRequest.getLeaderboard(username, topK));
            String jsonRes = gateway.sendRequest(jsonReq);
            
            Type type = new TypeToken<ServerResponse<LeaderboardDTO>>(){}.getType();
            ServerResponse<LeaderboardDTO> response = gson.fromJson(jsonRes, type);
            
            view.showResponse(response);
            if (response.getStatus() == Status.OK && response.getData() != null) {
                view.showLeaderboard(response.getData());
            }
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        }
    }

    /** Requests current player's career history and renders player stats. */
    public void handleHistory(NetworkGateway gateway) {
        try {
            String jsonReq = gson.toJson(HistoryRequest.getPlayerStats());
            String jsonRes = gateway.sendRequest(jsonReq);
            
            Type type = new TypeToken<ServerResponse<PlayerStatsDTO>>(){}.getType();
            ServerResponse<PlayerStatsDTO> response = gson.fromJson(jsonRes, type);
            
            view.showResponse(response);
            if (response.getStatus() == Status.OK && response.getData() != null) {
                view.showPlayerStats(response.getData());
            }
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        }
    }
}
