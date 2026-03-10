package com.connections.client.modules;

import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import com.connections.common.dto.*;
import com.connections.common.models.WordGroup;
import java.util.Arrays;
import java.util.List;

/**
 * Handles operations and display for the active game session.
 */
public class GameModuleView implements ResponseView {

    // --- Logic Constants ---
    private static final int MIN_TOKENS_SUBMIT = 2;
    private static final int MAX_MISTAKES = 4;
    private static final int MS_TO_S_RATIO = 1000;
    private static final int INDEX_WORDS_START = 1;

    public ClientRequest prepareSubmit(String[] tokens) {
        if (tokens.length < MIN_TOKENS_SUBMIT) {
            System.out.println("Usage: submit <w1> <w2> <w3> <w4>");
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

    public void displayResponse(ServerResponse<?> response) {
        System.out.println(response.getMessage());
        Object data = response.getData();
        if (data == null) {
            return;
        }

        if (data instanceof GameInfoDTO) {
            displayActiveGameInfo((GameInfoDTO) data);
        } else if (data instanceof GameStatsDTO) {
            displayActiveGameStats((GameStatsDTO) data);
        }
    }

    private void displayActiveGameInfo(GameInfoDTO info) {
        if (info.isFinished()) {
            System.out.println("Your game for this puzzle is over. Use 'history' for details.");
            return;
        }
        System.out.println("--- Current Game Status ---");
        System.out.println("Time remaining: " + (info.getRemainingTime() / MS_TO_S_RATIO) + "s");
        System.out.println(String.format("Mistakes: %d/%d", info.getMistakes(), MAX_MISTAKES));
        
        System.out.println("Groups found:");
        for (WordGroup group : info.getFoundGroups()) {
            System.out.println("  - " + group.getTheme() + ": " + String.join(", ", group.getWords()));
        }
        
        System.out.println("Remaining words: " + String.join(", ", info.getRemainingWords()));
        System.out.println("Current score: " + info.getScore());
    }

    private void displayActiveGameStats(GameStatsDTO stats) {
        System.out.println("--- Current Game Global Stats ---");
        System.out.println("Active players: " + stats.getActivePlayers());
        System.out.println("Players finished: " + stats.getFinishedPlayers());
        System.out.println("Winners: " + stats.getWonPlayers());
    }
}
