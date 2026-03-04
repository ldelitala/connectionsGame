package com.connections.client.modules;

import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import com.connections.common.dto.*;
import com.connections.common.models.WordGroup;
import java.util.List;

/**
 * Handles operations related to past games and global statistics.
 */
public class HistoryModule implements ClientModule {

    @Override
    public Request parseCommand(String command, String[] tokens) {
        switch (command.toLowerCase()) {
            case "info":
                return (tokens.length > 1) ? HistoryRequest.getGameInfo(Integer.parseInt(tokens[1])) : null;
            case "stats":
                return (tokens.length > 1) ? HistoryRequest.getGameStats(Integer.parseInt(tokens[1])) : null;
            case "ranking":
                Integer top = (tokens.length > 1) ? Integer.parseInt(tokens[1]) : 10;
                String player = (tokens.length > 2) ? tokens[2] : null;
                return HistoryRequest.getLeaderboard(player, top);
            case "history":
                return HistoryRequest.getPlayerStats();
            default:
                return null;
        }
    }

    @Override
    public void displayResponse(Response<?> response) {
        System.out.println(response.getMessage());
        Object data = response.getData();
        if (data == null) return;

        if (data instanceof GameInfoDTO) {
            displayConcludedGameInfo((GameInfoDTO) data);
        } else if (data instanceof GameStatsDTO) {
            displayConcludedGameStats((GameStatsDTO) data);
        } else if (data instanceof PlayerStatsDTO) {
            displayPlayerStats((PlayerStatsDTO) data);
        } else if (data instanceof LeaderboardDTO) {
            displayLeaderboard((LeaderboardDTO) data);
        }
    }

    private void displayConcludedGameInfo(GameInfoDTO info) {
        if (!info.isFinished()) {
            System.out.println("This game is still in progress. Use 'info' for details.");
            return;
        }
        System.out.println("--- Concluded Game Details (Game ID: " + info.getGameId() + ") ---");
        System.out.println("Score obtained: " + info.getScore());
        System.out.println("Mistakes made: " + info.getMistakes());
        System.out.println("Correct groups guessed: " + info.getCorrectGroupsCount() + "/4");
        System.out.println("Full word assignments:");
        for (WordGroup group : info.getAllGroups()) {
            System.out.println("  - " + group.getCategory() + ": " + String.join(", ", group.getWords()));
        }
    }

    private void displayConcludedGameStats(GameStatsDTO stats) {
        System.out.println("--- Concluded Game Global Statistics ---");
        System.out.println("Total participants: " + stats.getTotalParticipants());
        System.out.println("Finished participants: " + stats.getFinishedPlayers());
        System.out.println("Total winners: " + stats.getWonPlayers());
        System.out.println("Average score: " + stats.getAverageScore());
    }

    private void displayPlayerStats(PlayerStatsDTO stats) {
        System.out.println("--- Your Statistics ---");
        System.out.println("Total games played: " + stats.getTotalGamesPlayed());
        System.out.println("Total score: " + stats.getTotalScore());
        System.out.println("Average mistakes: " + stats.getAverageMistakes());
        System.out.println("Games finished: " + stats.getGamesFinished());
        System.out.println("Games won: " + stats.getGamesWon());
    }

    private void displayLeaderboard(LeaderboardDTO leaderboard) {
        System.out.println("--- Leaderboard ---");
        List<LeaderboardEntryDTO> entries = leaderboard.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDTO entry = entries.get(i);
            System.out.printf("%2d. %-15s %5d points
", (i + 1), entry.getUsername(), entry.getScore());
        }
    }
}
