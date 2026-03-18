package com.connections.client.ui.console;

import com.connections.common.protocol.server_responses.dto.*;
import com.connections.common.notifications.*;
import java.util.List;

/**
 * Static painters for CLI rendering. Separates the 'how' from the 'what'.
 * Uses Unicode characters and ANSI colors for a high-quality experience.
 * Java 8 compatible (uses manual string repetition instead of String.repeat).
 */
public class ConsoleRenderer {

    // --- ANSI COLORS ---
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String ITALIC = "\u001B[3m";

    // --- THEME COLORS (Original Game Palette) ---
    private static final String[] THEME_COLORS = { YELLOW, GREEN, BLUE, PURPLE };

    private static final int MS_TO_S_RATIO = 1000;
    private static final int MAX_MISTAKES = 4;

    public static void paintGuessResult(GuessResultDTO result) {
        System.out.println("\n" + BOLD + CYAN + "╔═══════════════════════════════════════════╗" + RESET);
        if ("success".equals(result.getStatus())) {
            System.out.println(BOLD + CYAN + "║" + GREEN + "  ★  CORRECT PROPOSAL!  ★                " + CYAN + "║" + RESET);
            System.out.println(BOLD + CYAN + "║" + RESET + "  Awarded: " + BOLD + GREEN + "+" + result.getPointsDelta() + RESET + " points.                  " + BOLD + CYAN + "║" + RESET);
        } else {
            System.out.println(BOLD + CYAN + "║" + RED + "  ✘  WRONG COMBINATION!  ✘               " + CYAN + "║" + RESET);
            System.out.println(BOLD + CYAN + "║" + RESET + "  Penalty: " + BOLD + RED + result.getPointsDelta() + RESET + " points.                   " + BOLD + CYAN + "║" + RESET);
        }
        System.out.printf(BOLD + CYAN + "║" + RESET + "  New Total Score: " + BOLD + "%-23d" + RESET + BOLD + CYAN + "║\n", result.getTotalPoints());
        System.out.println(BOLD + CYAN + "╚═══════════════════════════════════════════╝" + RESET);
        
        if (result.getRemainingWords() != null && !result.getRemainingWords().isEmpty()) {
            paintGrid(result.getRemainingWords());
        }
    }

    public static void paintPuzzleInfo(PuzzleStateDTO info) {
        String header = String.format("---  ROUND ID: #%d  ---", info.getPuzzleId());
        System.out.println("\n" + BOLD + BLUE + "┌" + repeat("─", 42) + "┐" + RESET);
        System.out.println(BOLD + BLUE + "│" + RESET + BOLD + CYAN + center(header, 42) + RESET + BOLD + BLUE + "│" + RESET);
        
        if (info.isFinished()) {
            System.out.println(BOLD + BLUE + "│" + RESET + GREEN + center("★ ROUND COMPLETED ★", 42) + RESET + BOLD + BLUE + "│" + RESET);
        } else {
            if (info.getRemainingTime() != null) {
                long sec = info.getRemainingTime() / MS_TO_S_RATIO;
                String tColor = (sec < 60) ? RED : YELLOW;
                System.out.println(BOLD + BLUE + "│" + RESET + " Time: " + BOLD + tColor + String.format("%3ds", sec) + RESET + repeat(" ", 32) + BOLD + BLUE + "│" + RESET);
            }
            int m = info.getMistakes();
            System.out.print(BOLD + BLUE + "│" + RESET + " Errors: ");
            for(int i=0; i<MAX_MISTAKES; i++) {
                if (i < m) System.out.print(RED + "● " + RESET);
                else System.out.print(GREEN + "○ " + RESET);
            }
            System.out.println(repeat(" ", 25) + BOLD + BLUE + "│" + RESET);
        }
        System.out.println(BOLD + BLUE + "└" + repeat("─", 42) + "┘" + RESET);

        // Found groups with theme colors
        if (info.getFoundGroups() != null && !info.getFoundGroups().isEmpty()) {
            System.out.println(BOLD + "  SOLVED CATEGORIES:" + RESET);
            for (int i = 0; i < info.getFoundGroups().size(); i++) {
                String color = THEME_COLORS[i % THEME_COLORS.length];
                System.out.println("  " + color + "■ " + RESET + BOLD + info.getFoundGroups().get(i) + RESET);
            }
        }

        // Full solution
        if (info.isFinished() && info.getAllGroups() != null && !info.getAllGroups().isEmpty()) {
            System.out.println("\n" + BOLD + "  ROUND REVEALED:" + RESET);
            for (int i = 0; i < info.getAllGroups().size(); i++) {
                com.connections.common.models.Category cat = info.getAllGroups().get(i);
                String color = THEME_COLORS[i % THEME_COLORS.length];
                System.out.println("  " + color + "■ " + BOLD + "[" + cat.getTheme() + "]: " + RESET + color + String.join(", ", cat.getWords()) + RESET);
            }
        }

        if (!info.isFinished() && info.getRemainingWords() != null && !info.getRemainingWords().isEmpty()) {
            System.out.println();
            paintGrid(info.getRemainingWords());
        }
    }

    private static void paintGrid(List<String> words) {
        System.out.println("  " + BOLD + "Available Words:" + RESET);
        System.out.println("  ╔" + repeat("═", 12) + "╤" + repeat("═", 12) + "╤" + repeat("═", 12) + "╤" + repeat("═", 12) + "╗");
        for (int i = 0; i < words.size(); i++) {
            if (i % 4 == 0) System.out.print("  ║ ");
            System.out.printf("%-10s " + (i%4==3 ? "║" : "│ "), words.get(i));
            if (i % 4 == 3 && i < words.size() - 1) {
                System.out.println("\n  ╟" + repeat("─", 12) + "┼" + repeat("─", 12) + "┼" + repeat("─", 12) + "┼" + repeat("─", 12) + "╢");
            }
        }
        System.out.println("\n  ╚" + repeat("═", 12) + "╧" + repeat("═", 12) + "╧" + repeat("═", 12) + "╧" + repeat("═", 12) + "╝");
    }

    public static void paintPuzzleStats(PuzzleStatsDTO stats) {
        System.out.println("\n" + BOLD + PURPLE + "  ╭" + repeat("─", 34) + "╮" + RESET);
        System.out.println(BOLD + PURPLE + "  │      GLOBAL ROUND STATISTICS     │" + RESET);
        System.out.println(BOLD + PURPLE + "  ├" + repeat("─", 34) + "┤" + RESET);
        System.out.printf("  │  Playing now:   %-16d │\n", (stats.getActivePlayers() != null ? stats.getActivePlayers() : 0));
        System.out.printf("  │  Completed:     %-16d │\n", (stats.getFinishedPlayers() != null ? stats.getFinishedPlayers() : 0));
        System.out.printf("  │  Winners:       " + GREEN + "%-16d" + RESET + " │\n", (stats.getWonPlayers() != null ? stats.getWonPlayers() : 0));
        System.out.println(BOLD + PURPLE + "  ╰" + repeat("─", 34) + "╯" + RESET);
    }

    public static void paintPlayerStats(PlayerStatsDTO stats) {
        System.out.println("\n" + BOLD + YELLOW + "  ┏" + repeat("━", 42) + "┓");
        System.out.println("  ┃          YOUR CAREER PROGRESSION         ┃");
        System.out.println("  ┣" + repeat("━", 42) + "┫");
        System.out.printf("  ┃  Completed Rounds:  %-20d ┃\n", stats.getPuzzlesCompleted());
        System.out.printf("  ┃  Victory Rate:      " + CYAN + "%-20s" + RESET + BOLD + YELLOW + " ┃\n", String.format("%.2f%%", stats.getWinRate()));
        System.out.printf("  ┃  Total Points:      " + GREEN + "%-20d" + RESET + BOLD + YELLOW + " ┃\n", stats.getTotalScore());
        System.out.printf("  ┃  Average Performance: %-18.2f ┃\n", stats.getAverageScore());
        System.out.println("  ┗" + repeat("━", 42) + "┛" + RESET);
    }

    public static void paintLeaderboard(LeaderboardDTO leaderboard) {
        System.out.println("\n" + BOLD + BLUE + "  ▛" + repeat("▀", 42) + "▜");
        System.out.println("  ▌            GLOBAL HALL OF FAME           ▐");
        System.out.println("  ▙" + repeat("▄", 42) + "▟");
        List<LeaderboardEntryDTO> entries = leaderboard.getEntries();
        if (entries == null || entries.isEmpty()) {
            System.out.println("  (No legends recorded yet)");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntryDTO entry = entries.get(i);
                String name = entry.getUsername();
                if (name.length() > 20) name = name.substring(0, 17) + "...";
                String color = (i < 3) ? YELLOW : RESET;
                System.out.printf("   " + color + "%2d. %-22s %7d pts" + RESET + "\n", (i + 1), name, entry.getScore());
            }
        }
    }

    public static void paintRank(LeaderboardEntryDTO e) {
        System.out.println("\n" + BOLD + "  >> POSITION: " + YELLOW + "#" + e.getRank() + RESET + 
                           " | NAME: " + CYAN + e.getUsername() + RESET + 
                           " | POINTS: " + BOLD + e.getScore() + RESET);
    }

    public static void paintNewPuzzleNotify(NewPuzzleNotification n) {
        System.out.println("\n" + BOLD + YELLOW + "  🔔 [NEWS] ROUND #" + n.getPuzzleId() + " HAS COMMENCED!" + RESET);
        paintGrid(n.getWords());
        System.out.println(ITALIC + "  Enter 'board' to examine the grid." + RESET);
    }

    public static void paintPuzzleEndNotify(PuzzleEndNotification n) {
        System.out.println("\n" + BOLD + RED + "  🏁 [NEWS] THE CURRENT ROUND HAS ENDED." + RESET);
        if (n.getStats() != null) paintPuzzleStats(n.getStats());
    }

    public static void paintPlayerSummaryNotify(PlayerPuzzleSummaryNotification n) {
        System.out.println("\n" + BOLD + BLUE + "  📊 [PERSONAL] ROUND SUMMARY ARCHIVED:" + RESET);
        if (n.getPersonalInfo() != null) paintPuzzleInfo(n.getPersonalInfo());
    }

    public static void paintWaitNotify(WaitNotification n) {
        String color = (n.getSecondsRemaining() <= 3) ? RED : YELLOW;
        System.out.print("\r" + BOLD + color + "  ⏳ NEXT ROUND IN: " + n.getSecondsRemaining() + "s   " + RESET);
        System.out.flush();
    }

    private static String center(String text, int width) {
        if (text.length() >= width) return text;
        int left = (width - text.length()) / 2;
        int right = width - text.length() - left;
        return repeat(" ", left) + text + repeat(" ", right);
    }

    private static String repeat(String s, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count * s.length());
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }
}
