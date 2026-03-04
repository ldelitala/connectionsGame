package com.connections.client;

import java.io.IOException;
import java.util.Scanner;

/**
 * Main entry point for the CLI client.
 * Orchestrates configuration, network setup, and starts the command controller.
 */
public class ClientMain {
    
    public static void main(String[] args) {
        ClientConfig config = new ClientConfig();
        Scanner scanner = new Scanner(System.in);
        ConnectionClient tcpClient = null;
        UdpNotificationListener udpListener = null;

        System.out.println("Welcome to Connections Game!");

        try {
            // 1. Setup TCP connection
            tcpClient = new ConnectionClient(config.getServerHost(), config.getServerPort());
            System.out.println("Connected to server at " + config.getServerHost() + ":" + config.getServerPort());

            // 2. Setup UDP listener (port from config or ephemeral)
            // For now we use the one from config, but could be dynamic
            int udpPort = 0; // Default or ephemeral
            udpListener = new UdpNotificationListener(udpPort);
            new Thread(udpListener).start();
            // Get the actual port if 0 was used
            udpPort = udpListener.getLocalPort();
            System.out.println("UDP Notification listener started on port " + udpPort);

            // 3. Start the Controller
            ClientController controller = new ClientController(tcpClient, scanner, udpPort);
            controller.start(scanner);

        } catch (IOException e) {
            System.err.println("Fatal Error: " + e.getMessage());
        } finally {
            // Cleanup
            try {
                if (tcpClient != null) tcpClient.close();
                if (udpListener != null) udpListener.stop();
            } catch (IOException ignored) {}
            scanner.close();
        }
    }
}
