package it.unipi.connections.server;

import it.unipi.connections.server.engine.GameEngine;
import it.unipi.connections.server.network.NioServer;
import it.unipi.connections.server.network.bridge.ProtocolBridge;

import java.io.IOException;

/**
 * Modern Entry point for the Connections Server.
 * Architecture: Engine (Core) -> Bridge (Protocol) -> Core (Nio).
 */
public class ServerMain {
    public static void main(String[] args) {

        System.out.println("--- Starting Connections Server ---");

        ServerConfig config = (args.length > 0) ? new ServerConfig(args[0]) : new ServerConfig();

        try {

            GameEngine engine = new GameEngine(config);
            engine.start();

            ProtocolBridge bridge = new ProtocolBridge(engine);

            NioServer server = new NioServer(config.getTcpPort(), bridge, config.getThreadPoolSize());

            // --- GRACEFUL SHUTDOWN HOOK ---
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n--- Shutdown signal received. Closing server... ---");
                server.stop();
                engine.stop();
                System.out.println("--- Server stopped gracefully. ---");
            }));

            System.out.println("Server ready. TCP Port: " + config.getTcpPort());
            server.start();

        } catch (IOException e) {
            System.err.println("Critical IO error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected server error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
