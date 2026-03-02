package com.connections.server;

import java.io.IOException;

/**
 * Main entry point for the Connections Server.
 * 
 * Possible command line arguments:
 * - args[0]: (Optional) Path to the server configuration file. 
 *            Default if not provided.
 */
public class ServerMain {
    public static void main(String[] args) {
        System.out.println("Starting Connections Server...");
        
        ServerConfig config = (args.length > 0) ? new ServerConfig(args[0]) : new ServerConfig();
        
        try {
            GamesDAO gamesDAO = new GamesDAO(config.getGameDataPath());
            UserManager userManager = new UserManager(config.getUserDataPath());
            UdpNotifier udpNotifier = new UdpNotifier();
            GameSessionManager sessionManager = new GameSessionManager(gamesDAO, userManager, udpNotifier, config.getGameDurationMillis());
            
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
