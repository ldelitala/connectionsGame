package com.connections.client.ui.console;

import com.connections.client.ui.InputProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * CLI implementation of InputProvider.
 * Parses raw string input from the console into explicit Game Engine actions.
 */
public class ConsoleInputProvider implements InputProvider {

    private static final String REGEX_WHITESPACE = "\\s+";
    
    // Commands mapping
    private static final String CMD_REGISTER = "register";
    private static final String CMD_LOGIN = "login";
    private static final String CMD_LOGOUT = "logout";
    private static final String CMD_UPDATE = "update";
    private static final String CMD_GUESS = "guess";
    private static final String CMD_BOARD = "board";
    private static final String CMD_STATS = "stats";
    private static final String CMD_INFO = "info";
    private static final String CMD_GLOBAL = "global";
    private static final String CMD_RANKING = "ranking";
    private static final String CMD_HISTORY = "history";
    private static final String CMD_HELP = "help";
    private static final String CMD_EXIT = "exit";

    private final Scanner scanner;
    private volatile boolean running = true;

    public ConsoleInputProvider() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void startListening(InputListener listener) {
        while (running && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            processLine(line, listener);
        }
    }

    private void processLine(String line, InputListener listener) {
        String[] tokens = line.split(REGEX_WHITESPACE);
        String command = tokens[0].toLowerCase();

        try {
            switch (command) {
                case CMD_REGISTER:
                    if (tokens.length < 3) throw new IllegalArgumentException("Usage: register <user> <psw>");
                    listener.onRegister(tokens[1], tokens[2]);
                    break;
                case CMD_LOGIN:
                    if (tokens.length < 3) throw new IllegalArgumentException("Usage: login <user> <psw>");
                    listener.onLogin(tokens[1], tokens[2]);
                    break;
                case CMD_LOGOUT:
                    listener.onLogout();
                    break;
                case CMD_UPDATE:
                    if (tokens.length < 4) throw new IllegalArgumentException("Usage: update <oldP> <newU> <newP>");
                    listener.onUpdateCredentials(tokens[1], tokens[2], tokens[3]);
                    break;
                case CMD_GUESS:
                    if (tokens.length < 2) throw new IllegalArgumentException("Usage: guess <w1> <w2> <w3> <w4>");
                    listener.onGuess(Arrays.asList(Arrays.copyOfRange(tokens, 1, tokens.length)));
                    break;
                case CMD_BOARD:
                    listener.onRequestBoard();
                    break;
                case CMD_STATS:
                    listener.onRequestStats();
                    break;
                case CMD_INFO:
                    if (tokens.length < 2) throw new IllegalArgumentException("Usage: info <puzzleId>");
                    listener.onRequestPastInfo(Integer.parseInt(tokens[1]));
                    break;
                case CMD_GLOBAL:
                    if (tokens.length < 2) throw new IllegalArgumentException("Usage: global <puzzleId>");
                    listener.onRequestGlobalStats(Integer.parseInt(tokens[1]));
                    break;
                case CMD_RANKING:
                    handleRanking(tokens, listener);
                    break;
                case CMD_HISTORY:
                    listener.onRequestHistory();
                    break;
                case CMD_HELP:
                    listener.onHelp();
                    break;
                case CMD_EXIT:
                    listener.onExit();
                    break;
                default:
                    System.out.println("Unknown command. Type 'help' for assistance.");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Input error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing input: " + e.getMessage());
        }
    }

    private void handleRanking(String[] tokens, InputListener listener) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Usage: ranking <user> OR ranking -top [k]");
        }
        if (tokens[1].equalsIgnoreCase("-top")) {
            Integer k = (tokens.length > 2) ? Integer.parseInt(tokens[2]) : null;
            listener.onRequestRanking(null, k);
        } else {
            listener.onRequestRanking(tokens[1], null);
        }
    }

    @Override
    public void stop() { this.running = false; }

    @Override
    public void close() { stop(); scanner.close(); }
}
