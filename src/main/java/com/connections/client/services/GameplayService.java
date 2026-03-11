package com.connections.client.services;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import java.util.List;

/**
 * Handles operations related to active puzzle gameplay (guessing, status).
 */
public class GameplayService {

    private final ClientView view;

    public GameplayService(ClientView view) {
        this.view = view;
    }

    /** Prepares a guess (proposal) request. */
    public ClientRequest prepareGuess(List<String> words) {
        return GuessRequest.submitProposal(words);
    }

    /** Prepares a request for current puzzle state. */
    public ClientRequest prepareStatus() {
        return GuessRequest.getCurrentInfo();
    }

    /** Prepares a request for current puzzle global stats. */
    public ClientRequest prepareStats() {
        return GuessRequest.getCurrentStats();
    }

    /** Interprets and displays gameplay-related responses. */
    public void handleResponse(ServerResponse<?> response) {
        view.showResponse(response);
        Object data = response.getData();
        if (data instanceof PuzzleStateDTO) {
            view.showGameInfo((PuzzleStateDTO) data);
        } else if (data instanceof PuzzleStatsDTO) {
            view.showGameStats((PuzzleStatsDTO) data);
        }
    }
}
