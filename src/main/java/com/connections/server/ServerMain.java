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
            PuzzlesDAO puzzlesDAO = new PuzzlesDAO(config.getPuzzleDataPath());
            UserManager userManager = new UserManager(config.getUserDataPath());
            UdpNotifier udpNotifier = new UdpNotifier();
            PuzzleSessionManager sessionManager = new PuzzleSessionManager(puzzlesDAO, userManager, udpNotifier, config.getPuzzleDurationMillis());
            
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
