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
 * Manages JSON mapping to specific DTOs and view rendering.
 */
public class HistoryAdapter {

    private final ClientView view;
    private final Gson gson = new Gson();

    public HistoryAdapter(ClientView view) {
        this.view = view;
    }

    /** Requests past puzzle info. */
    public void handlePastInfo(int puzzleId, NetworkGateway gateway) {
        ClientRequest req = HistoryRequest.getGameInfo(puzzleId);
        callAndProcess(req, gateway, new TypeToken<ServerResponse<PuzzleStateDTO>>(){}.getType());
    }

    /** Requests global stats for a past puzzle. */
    public void handleGlobalStats(int puzzleId, NetworkGateway gateway) {
        ClientRequest req = HistoryRequest.getGameStats(puzzleId);
        callAndProcess(req, gateway, new TypeToken<ServerResponse<PuzzleStatsDTO>>(){}.getType());
    }

    /** Requests leaderboard or player ranking. */
    public void handleRanking(String username, Integer topK, NetworkGateway gateway) {
        ClientRequest req = HistoryRequest.getLeaderboard(username, topK);
        callAndProcess(req, gateway, new TypeToken<ServerResponse<LeaderboardDTO>>(){}.getType());
    }

    /** Requests current player's career history. */
    public void handleHistory(NetworkGateway gateway) {
        ClientRequest req = HistoryRequest.getPlayerStats();
        callAndProcess(req, gateway, new TypeToken<ServerResponse<PlayerStatsDTO>>(){}.getType());
    }

    /** Helper for archive-related responses. */
    private <T extends DTO> void callAndProcess(ClientRequest request, NetworkGateway gateway, Type type) {
        try {
            String json = gateway.sendRequest(request);
            ServerResponse<T> response = gson.fromJson(json, type);
            
            view.showResponse(response);
            if (response.getStatus() == Status.OK && response.getData() != null) {
                T data = response.getData();
                if (data instanceof PuzzleStateDTO) view.showGameInfo((PuzzleStateDTO) data);
                else if (data instanceof PuzzleStatsDTO) view.showGameStats((PuzzleStatsDTO) data);
                else if (data instanceof PlayerStatsDTO) view.showPlayerStats((PlayerStatsDTO) data);
                else if (data instanceof LeaderboardDTO) view.showLeaderboard((LeaderboardDTO) data);
            }
        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        }
    }
}
