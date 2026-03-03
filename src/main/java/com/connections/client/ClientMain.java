package com.connections.client;

import java.io.IOException;
import java.util.Scanner;

/**
 * Main entry point for the CLI client.
 */
public class ClientMain {
    private static ConnectionClient client;
    private static ClientConfig config;
    private static ClientCommandHandler commandHandler;

    public static void main(String[] args) {
        config = new ClientConfig();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Connections Game!");

        // Automatic connection on startup
        try {
            client = new ConnectionClient(config.getServerHost(), config.getServerPort());
            System.out.println("Connected to server at " + config.getServerHost() + ":" + config.getServerPort());
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            System.err.println("Please check your config/client.properties and restart.");
        }

        commandHandler = new ClientCommandHandler(client);

        while (true) {
            String currentUsername = commandHandler.getCurrentUsername();
            System.out.print(currentUsername != null ? "[" + currentUsername + "] > " : "> ");
            
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] tokens = input.split("\\s+");

            try {
                boolean continueLoop = commandHandler.execute(tokens, scanner);
                if (!continueLoop) {
                    break;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        // Cleanup
        try {
            if (client != null) client.close();
        } catch (IOException ignored) {}
    }
}
