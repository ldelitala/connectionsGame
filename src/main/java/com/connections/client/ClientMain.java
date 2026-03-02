package com.connections.client;

import com.connections.common.*;
import com.connections.common.dto.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Main entry point for the CLI client.
 */
public class ClientMain {
    private static ConnectionClient client;
    private static UdpNotificationListener udpListener;
    private static String currentUsername = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Connections Game!");

        while (true) {
            System.out.print(currentUsername != null ? "[" + currentUsername + "] > " : "> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] tokens = input.split("\\s+");
            String command = tokens[0];

            try {
                switch (command) {
                    case "connect":
                        handleConnect(tokens);
                        break;
                    case "register":
                        handleRegister(tokens, scanner);
                        break;
                    case "login":
                        handleLogin(tokens, scanner);
                        break;
                    case "logout":
                        handleLogout();
                        break;
                    case "update":
                        handleUpdateCredentials(scanner);
                        break;
                    case "play":
                        handlePlay(scanner);
                        break;
                    case "game":
                        handleRequestGameInfo(tokens);
                        break;
                    case "stats":
                        handleRequestPlayerStats();
                        break;
                    case "rank":
                        handleRequestLeaderboard(tokens);
                        break;
                    case "exit":
                        if (client != null) client.close();
                        System.out.println("Goodbye!");
                        return;
                    case "help":
                        printHelp();
                        break;
                    default:
                        System.out.println("Unknown command. Type 'help' for available commands.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private static void handleConnect(String[] tokens) throws IOException {
        String host = (tokens.length > 1) ? tokens[1] : "localhost";
        int port = (tokens.length > 2) ? Integer.parseInt(tokens[2]) : 8080;
        client = new ConnectionClient(host, port);
        System.out.println("Connected to " + host + ":" + port);
    }

    private static void ensureConnected() throws IOException {
        if (client == null) throw new IOException("Not connected. Use 'connect' first.");
    }

    private static void handleRegister(String[] tokens, Scanner scanner) throws IOException {
        ensureConnected();
        String user, pass;
        if (tokens.length >= 3) {
            user = tokens[1];
            pass = tokens[2];
        } else {
            System.out.print("Username: ");
            user = scanner.nextLine().trim();
            System.out.print("Password: ");
            pass = scanner.nextLine().trim();
        }

        UserRequest req = new UserRequest(Operation.REGISTER, user, pass, null, null, null, null);
        Response resp = client.sendRequest(req);
        System.out.println(resp.getMessage());
    }

    private static void handleLogin(String[] tokens, Scanner scanner) throws IOException {
        ensureConnected();
        String user, pass;
        if (tokens.length >= 3) {
            user = tokens[1];
            pass = tokens[2];
        } else {
            System.out.print("Username: ");
            user = scanner.nextLine().trim();
            System.out.print("Password: ");
            pass = scanner.nextLine().trim();
        }

        int udpPort = 0; // In a real app, find an available port
        UserRequest req = new UserRequest(Operation.LOGIN, user, pass, null, null, null, udpPort);
        Response resp = client.sendRequest(req);
        
        if (resp.getStatus().equals("ok")) {
            currentUsername = user;
            System.out.println("Logged in as " + user);
            // Start UDP listener (simplified for CLI demo)
            // if (udpListener != null) udpListener.stop();
            // udpListener = new UdpNotificationListener(udpPort);
            // new Thread(udpListener).start();
        } else {
            System.out.println("Login failed: " + resp.getMessage());
        }
    }

    private static void handleLogout() throws IOException {
        ensureConnected();
        Response resp = client.sendRequest(new UserRequest(Operation.LOGOUT, null, null, null, null, null, null));
        if (resp.getStatus().equals("ok")) {
            currentUsername = null;
        }
        System.out.println(resp.getMessage());
    }

    private static void handleUpdateCredentials(Scanner scanner) throws IOException {
        ensureConnected();
        System.out.print("New username (leave empty to keep current): ");
        String newUname = scanner.nextLine().trim();
        if (newUname.isEmpty()) newUname = null;
        
        System.out.print("Current password: ");
        String oldPass = scanner.nextLine().trim();
        System.out.print("New password (leave empty to keep current): ");
        String newPass = scanner.nextLine().trim();
        if (newPass.isEmpty()) newPass = null;

        UserRequest req = new UserRequest(Operation.UPDATE_CREDENTIALS, null, null, newUname, oldPass, newPass, null);
        Response resp = client.sendRequest(req);
        
        if (resp.getStatus().equals("ok") && newUname != null) {
            currentUsername = newUname;
        }
        System.out.println(resp.getMessage());
    }

    private static void handlePlay(Scanner scanner) throws IOException {
        ensureConnected();
        System.out.println("Enter 4 words separated by spaces:");
        String input = scanner.nextLine().trim();
        List<String> words = Arrays.asList(input.split("\\s+"));
        
        if (words.size() != 4) {
            System.out.println("Error: exactly 4 words required.");
            return;
        }

        GameRequest req = new GameRequest(Operation.SUBMIT_PROPOSAL, words);
        Response resp = client.sendRequest(req);
        System.out.println(resp.getMessage());
        // For droids/debugging: client serializes ProposalResultDTO into 'data' field
    }

    private static void handleRequestGameInfo(String[] tokens) throws IOException {
        ensureConnected();
        Integer gameId = (tokens.length > 1) ? Integer.parseInt(tokens[1]) : null;
        InfoRequest req = new InfoRequest(Operation.REQUEST_GAME_INFO, gameId, null, null);
        Response resp = client.sendRequest(req);
        System.out.println(resp.getMessage());
        if (resp.getData() != null) {
            System.out.println("Data: " + resp.getData());
        }
    }

    private static void handleRequestPlayerStats() throws IOException {
        ensureConnected();
        InfoRequest req = new InfoRequest(Operation.REQUEST_PLAYER_STATS, null, null, null);
        Response resp = client.sendRequest(req);
        System.out.println(resp.getMessage());
        if (resp.getData() != null) {
            System.out.println("Stats: " + resp.getData());
        }
    }

    private static void handleRequestLeaderboard(String[] tokens) throws IOException {
        ensureConnected();
        Integer top = (tokens.length > 1) ? Integer.parseInt(tokens[1]) : 10;
        InfoRequest req = new InfoRequest(Operation.REQUEST_LEADERBOARD, null, null, top);
        Response resp = client.sendRequest(req);
        System.out.println(resp.getMessage());
        if (resp.getData() != null) {
            System.out.println("Leaderboard: " + resp.getData());
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  connect [host] [port]   - Connect to the server");
        System.out.println("  register [user] [pass]  - Register a new account");
        System.out.println("  login [user] [pass]     - Log in to your account");
        System.out.println("  logout                  - Log out of your account");
        System.out.println("  update                  - Update your username or password");
        System.out.println("  play                    - Submit a group proposal");
        System.out.println("  game [id]               - View info about a game (yours or global)");
        System.out.println("  stats                   - View your personal statistics");
        System.out.println("  rank [k]                - View top K players");
        System.out.println("  exit                    - Exit the application");
    }
}
