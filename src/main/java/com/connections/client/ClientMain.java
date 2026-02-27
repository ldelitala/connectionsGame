package com.connections.client;

import com.connections.common.Request;
import com.connections.common.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Main CLI for the Connections Client.
 */
public class ClientMain {
    private static ConnectionClient client;
    private static UdpNotificationListener udpListener;
    private static String loggedInUser = null;

    public static void main(String[] args) {
        System.out.println("=== Connections Game CLI ===");
        
        java.util.Properties props = new java.util.Properties();
        String host = "localhost";
        int port = 8080;
        
        try (java.io.InputStream input = new java.io.FileInputStream("config/client.properties")) {
            props.load(input);
            host = props.getProperty("client.server_host", "localhost");
            port = Integer.parseInt(props.getProperty("client.server_port", "8080"));
            System.out.println("Loaded client config from config/client.properties");
        } catch (java.io.IOException | NumberFormatException e) {
            System.err.println("Could not load client properties, using defaults.");
        }

        // Allow command line overrides
        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
            client = new ConnectionClient(host, port);
            client.connect();
            System.out.println("Connected to server at " + host + ":" + port);
            
            // Start UDP listener on an ephemeral port
            udpListener = new UdpNotificationListener(0); 
            udpListener.start();

            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                printPrompt();
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                String command = parts[0].toLowerCase();

                try {
                    switch (command) {
                        case "register":
                            handleRegister(parts);
                            break;
                        case "login":
                            handleLogin(parts);
                            break;
                        case "logout":
                            handleLogout();
                            break;
                        case "play":
                            handlePlay();
                            break;
                        case "submit":
                            handleSubmit(parts);
                            break;
                        case "stats":
                            handleStats();
                            break;
                        case "rank":
                            handleLeaderboard();
                            break;
                        case "update":
                            handleUpdate(parts);
                            break;
                        case "help":
                            printHelp();
                            break;
                        case "exit":
                            running = false;
                            break;
                        default:
                            System.out.println("Unknown command. Type 'help' for available commands.");
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }

            if (loggedInUser != null) handleLogout();
            udpListener.stopListener();
            client.close();
            System.out.println("Goodbye!");

        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }
    }

    private static void printPrompt() {
        if (loggedInUser != null) {
            System.out.print("[" + loggedInUser + "] > ");
        } else {
            System.out.print("> ");
        }
    }

    private static void handleRegister(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Usage: register <username> <password>");
            return;
        }
        Request req = new Request("register");
        req.setUsername(parts[1]);
        req.setPassword(parts[2]);
        Response res = client.sendRequest(req);
        System.out.println(res.getMessage());
    }

    private static void handleLogin(String[] parts) throws IOException {
        if (parts.length < 3) {
            System.out.println("Usage: login <username> <password>");
            return;
        }
        Request req = new Request("login");
        req.setUsername(parts[1]);
        req.setPassword(parts[2]);
        req.setUdpPort(udpListener.getPort());
        Response res = client.sendRequest(req);
        if (res.getStatus().equals("ok")) {
            loggedInUser = parts[1];
        }
        System.out.println(res.getMessage());
    }

    private static void handleLogout() throws IOException {
        if (loggedInUser == null) {
            System.out.println("Not logged in.");
            return;
        }
        Request req = new Request("logout");
        Response res = client.sendRequest(req);
        System.out.println(res.getMessage());
        loggedInUser = null;
    }

    private static void handleUpdate(String[] parts) throws IOException {
        if (loggedInUser == null) {
            System.out.println("Please login first.");
            return;
        }
        if (parts.length < 3) {
            System.out.println("Usage: update [newUsername] [newPassword]");
            return;
        }
        Request req = new Request("updateCredentials");
        req.setOldUsername(loggedInUser);
        // Simple heuristic: if 2 args, assume second is new username OR new pass?
        // Let's make it explicit for simplicity: update <newUsername> <newPassword>
        if (parts.length >= 2) req.setNewUsername(parts[1]);
        if (parts.length >= 3) req.setNewPassword(parts[2]);
        
        System.out.print("Confirm current password: ");
        Scanner scanner = new Scanner(System.in);
        String currentPass = scanner.nextLine().trim();
        req.setOldPassword(currentPass);

        Response res = client.sendRequest(req);
        System.out.println(res.getMessage());
        if (res.getStatus().equals("ok") && req.getNewUsername() != null) {
            loggedInUser = req.getNewUsername();
        }
    }

    private static void handlePlay() throws IOException {
        if (loggedInUser == null) {
            System.out.println("Please login first.");
            return;
        }
        Request req = new Request("requestGameInfo");
        Response res = client.sendRequest(req);
        if (res.getStatus().equals("ok")) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) res.getData();
            System.out.println("Game ID: " + data.get("gameId"));
            System.out.println("Words: " + data.get("words"));
            System.out.println("Remaining time: " + (Double.valueOf(data.get("remainingTime").toString()).longValue() / 1000) + "s");
        } else {
            System.out.println(res.getMessage());
        }
    }

    private static void handleSubmit(String[] parts) throws IOException {
        if (loggedInUser == null) {
            System.out.println("Please login first.");
            return;
        }
        if (parts.length < 5) {
            System.out.println("Usage: submit <word1> <word2> <word3> <word4>");
            return;
        }
        Request req = new Request("submitProposal");
        req.setWords(Arrays.asList(parts[1], parts[2], parts[3], parts[4]));
        Response res = client.sendRequest(req);
        System.out.println(res.getMessage());
        if (res.getData() != null) {
            System.out.println("Result: " + res.getData());
        }
    }

    private static void handleStats() throws IOException {
        if (loggedInUser == null) {
            System.out.println("Please login first.");
            return;
        }
        Request req = new Request("requestPlayerStats");
        Response res = client.sendRequest(req);
        System.out.println("Stats: " + res.getData());
    }

    private static void handleLeaderboard() throws IOException {
        if (loggedInUser == null) {
            System.out.println("Please login first.");
            return;
        }
        Request req = new Request("requestLeaderboard");
        Response res = client.sendRequest(req);
        System.out.println("Leaderboard: " + res.getData());
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  register <user> <pass>  - Create a new account");
        System.out.println("  login <user> <pass>     - Login to your account");
        System.out.println("  logout                  - Logout");
        System.out.println("  play                    - Show current game info");
        System.out.println("  submit <w1> <w2> <w3> <w4> - Submit a word group");
        System.out.println("  stats                   - View your personal statistics");
        System.out.println("  rank                    - View top 10 players");
        System.out.println("  exit                    - Exit the application");
    }
}
