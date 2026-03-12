package com.connections.server;

import com.connections.server.core.NioServer;
import com.connections.server.handler.RequestDispatcher;
import com.connections.server.logic.*;
import com.connections.server.persistence.*;

import java.io.IOException;

/**
 * Modern Entry point for the Connections Server.
 * Initializes the decoupled architecture: Persistence -> Logic -> Handler -> Core.
 */
public class ServerMain {
    public static void main(String[] args) {
        System.out.println("--- Starting Connections Server (NIO Refactored) ---");
        
        ServerConfig config = (args.length > 0) ? new ServerConfig(args[0]) : new ServerConfig();
        
        try {
            // 1. Persistence Layer
            PuzzlesDAO puzzlesDAO = new PuzzlesDAO(config.getPuzzleDataPath());
            UsersDAO usersDAO = new UsersDAO(config.getUserDataPath());
            HistoryDAO historyDAO = new HistoryDAO(config.getLogPath());
            
            // 2. Logic Layer (Coordinators)
            UdpNotifier udpNotifier = new UdpNotifier();
            HistoryCoordinator historyCoordinator = new HistoryCoordinator(historyDAO, usersDAO);
            UserCoordinator userCoordinator = new UserCoordinator(usersDAO, udpNotifier, historyCoordinator);
            PuzzleCoordinator puzzleCoordinator = new PuzzleCoordinator(
                    puzzlesDAO, historyCoordinator, userCoordinator, udpNotifier, config.getPuzzleDurationMillis());
            
            // 3. Handler Layer
            RequestDispatcher dispatcher = new RequestDispatcher(
                    userCoordinator, puzzleCoordinator, historyCoordinator, config.getThreadPoolSize());
            
            // 4. Network Layer (NIO Core)
            NioServer server = new NioServer(config.getTcpPort(), dispatcher);
            
            server.start();
            
        } catch (IOException e) {
            System.err.println("Critical server error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
