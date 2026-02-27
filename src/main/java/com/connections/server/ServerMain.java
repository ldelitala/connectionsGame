package com.connections.server;

import java.io.IOException;

/**
 * Main entry point for the Connections Server.
 * 
 * Possible command line arguments:
 * - args[0]: (Optional) Path to the server configuration file. 
 *            Defaults to "config/server.properties" if not provided.
 */
public class ServerMain {
    public static void main(String[] args) {
        System.out.println("Starting Connections Server...");
        
        String configPath = (args.length > 0) ? args[0] : "config/server.properties";
        ServerConfig config = new ServerConfig(configPath);
        
        try {
            // Initialize Managers
            GamesDAO gamesDAO = new GamesDAO(config.getGameDataPath());
            UserManager userManager = new UserManager(config.getUserDataPath());
            UdpNotifier udpNotifier = new UdpNotifier();
            GameSessionManager sessionManager = new GameSessionManager(gamesDAO, userManager, udpNotifier);
            
            // Initialize and start TCP Server
            ConnectionsServer server = new ConnectionsServer(config, userManager, sessionManager, udpNotifier);
            server.start();
            
        } catch (IOException e) {
            System.err.println("Critical server error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
