package com.connections.client.services;

import com.connections.client.ui.ClientView;
import com.connections.common.dto.*;
import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import java.util.Arrays;
import java.util.List;

/**
 * Handles operations and display for the active puzzle session.
 */
public class GameplayService {

    private static final int MIN_TOKENS_SUBMIT = 2;
    private static final int INDEX_WORDS_START = 1;

    private final ClientView view;

    /**
     * Initializes the gameplay service.
     *
     * @param view The UI view to display game information and results.
     */
    public GameplayService(ClientView view) {
        this.view = view;
    }

    /**
     * Prepares a guess submission request.
     *
     * @param tokens The command tokens (expected: "guess", word1, word2, word3, word4).
     * @return A {@link ClientRequest} for the guess, or null if insufficient words.
     */
    public ClientRequest prepareSubmit(String[] tokens) {
        if (tokens.length < MIN_TOKENS_SUBMIT) {
            view.showMessage("Usage: guess <w1> <w2> <w3> <w4>");
            return null;
        }
        List<String> words = Arrays.asList(Arrays.copyOfRange(tokens, INDEX_WORDS_START, tokens.length));
        return GuessRequest.submitProposal(words);
    }

    /**
     * Prepares a request for the current status of the active puzzle board.
     *
     * @return A {@link ClientRequest} for puzzle status.
     */
    public ClientRequest prepareStatus() {
        return GuessRequest.getCurrentInfo();
    }

    /**
     * Prepares a request for global statistics of the current active puzzle.
     *
     * @return A {@link ClientRequest} for puzzle statistics.
     */
    public ClientRequest prepareStats() {
        return GuessRequest.getCurrentStats();
    }

    /**
     * Processes a server response related to gameplay and updates the view.
     *
     * @param response The server response to handle.
     */
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
