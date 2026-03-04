package com.connections.client.modules;

import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import com.connections.common.dto.*;
import com.connections.common.models.WordGroup;
import java.util.Arrays;
import java.util.List;

/**
 * Handles operations related to the active game session.
 */
public class GameModule implements ClientModule {

    @Override
    public Request parseCommand(String command, String[] tokens) {
        switch (command.toLowerCase()) {
            case "submit":
                return handleSubmit(tokens);
            case "info":
                // If it's just "info", it refers to the current game
                return (tokens.length == 1) ? GameRequest.getCurrentInfo() : null;
            case "stats":
                // If it's just "stats", it refers to the current game
                return (tokens.length == 1) ? GameRequest.getCurrentStats() : null;
            default:
                return null;
        }
    }

    private Request handleSubmit(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Usage: submit word1 word2 word3 word4");
            return null;
        }
        List<String> words = Arrays.asList(Arrays.copyOfRange(tokens, 1, tokens.length));
        return GameRequest.submitProposal(words);
    }

    @Override
    public void displayResponse(Response<?> response) {
        System.out.println(response.getMessage());
        Object data = response.getData();
        if (data == null) return;

        if (data instanceof GameInfoDTO) {
            displayGameInfo((GameInfoDTO) data);
        } else if (data instanceof GameStatsDTO) {
            displayGameStats((GameStatsDTO) data);
        }
    }

    private void displayGameInfo(GameInfoDTO info) {
        if (info.isFinished()) {
            System.out.println("The game is already finished. Use 'history' for details.");
            return;
        }
        System.out.println("--- Current Game Status ---");
        System.out.println("Time left: " + (info.getRemainingTime() / 1000) + "s");
        System.out.println("Mistakes: " + info.getMistakes() + "/4");
        System.out.println("Found groups:");
        for (WordGroup group : info.getFoundGroups()) {
            System.out.println("  - " + group.getCategory() + ": " + String.join(", ", group.getWords()));
        }
        System.out.println("Remaining words: " + String.join(", ", info.getRemainingWords()));
        System.out.println("Score: " + info.getScore());
    }

    private void displayGameStats(GameStatsDTO stats) {
        System.out.println("--- Current Game Statistics ---");
        System.out.println("Active players: " + stats.getActivePlayers());
        System.out.println("Finished players: " + stats.getFinishedPlayers());
        System.out.println("Winners: " + stats.getWonPlayers());
    }
}
