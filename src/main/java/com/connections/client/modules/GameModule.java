package com.connections.client.modules;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import java.util.Arrays;
import java.util.List;

/**
 * Handles operations and display for the active game session.
 */
public class GameModule {

    private static final int MIN_TOKENS_SUBMIT = 2;
    private static final int INDEX_WORDS_START = 1;

    public ClientRequest prepareSubmit(String[] tokens, ClientView view) {
        if (tokens.length < MIN_TOKENS_SUBMIT) {
            view.showMessage("Usage: submit <w1> <w2> <w3> <w4>");
            return null;
        }
        List<String> words = Arrays.asList(Arrays.copyOfRange(tokens, INDEX_WORDS_START, tokens.length));
        return GameRequest.submitProposal(words);
    }

    public ClientRequest prepareStatus() {
        return GameRequest.getCurrentInfo();
    }

    public ClientRequest prepareStats() {
        return GameRequest.getCurrentStats();
    }

    public void handleResponse(ServerResponse<?> response, ClientView view) {
        view.showResponse(response);
        Object data = response.getData();
        if (data instanceof GameInfoDTO) {
            view.showGameInfo((GameInfoDTO) data);
        } else if (data instanceof GameStatsDTO) {
            view.showGameStats((GameStatsDTO) data);
        }
    }
}
