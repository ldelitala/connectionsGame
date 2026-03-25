package it.unipi.connections.client;

import it.unipi.connections.client.network.NotificationService;
import it.unipi.connections.client.network.NetworkGateway;
import it.unipi.connections.client.engine.GameEngine;
import it.unipi.connections.client.ui.*;
import it.unipi.connections.client.ui.console.*;
import java.io.IOException;

/**
 * Main entry point for the Client.
 */
public class ClientMain {

    private static volatile GameEngine currentEngine;

    public static void main(String[] args) {
        ClientConfig config = (args.length > 0) ? new ClientConfig(args[0]) : new ClientConfig();
        ClientView view = new ConsoleView();

        view.showMessage("Welcome to Connections Engine!");

        // --- SHUTDOWN HOOK ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (currentEngine != null) {
                System.out.println("\n[System] Shutdown signal detected. Cleaning up...");
                currentEngine.onExit();
            }
        }));

        try (
            NetworkGateway gateway = new NetworkGateway(config.getServerHost(), config.getServerPort());
            NotificationService notificationService = new NotificationService(config.getUdpPort(), view);
            InputProvider inputProvider = new ConsoleInputProvider()
        ) {
            view.showMessage("Connected to server: " + config.getServerHost() + ":" + config.getServerPort());

            notificationService.start();
            int actualUdpPort = notificationService.getLocalPort();
            view.showMessage("Notification service active on port " + actualUdpPort);

            currentEngine = new GameEngine(gateway, actualUdpPort, view, inputProvider);
            currentEngine.mainLoop();

        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        } catch (Exception e) {
            view.showError("Unexpected error: " + e.getMessage());
        } finally {
            view.showMessage("Engine terminated.");
        }
    }
}
