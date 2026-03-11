package com.connections.client;

import com.connections.client.network.NotificationService;
import com.connections.client.network.NetworkGateway;
import com.connections.client.controller.GameEngine;
import com.connections.client.ui.*;
import com.connections.client.ui.console.*;
import java.io.IOException;

/**
 * Main entry point for the Client. Initializes configuration, sets up network communication, starts
 * the notification service, and launches the game engine. Handles exceptions gracefully and ensures
 * resources are cleaned up on shutdown.
 */
public class ClientMain {

    /**
     * The application entry point.
     * Parses command-line arguments for configuration, initializes core components,
     * and enters the game loop.
     *
     * @param args Command-line arguments. The first argument can be a custom path to a properties file.
     */
    public static void main(String[] args) {

        ClientConfig config = (args.length > 0) ? new ClientConfig(args[0]) : new ClientConfig();

        ClientView view = new ConsoleView();

        view.showMessage("Welcome to Connections Engine!");

        try (

                NetworkGateway gateway =
                        new NetworkGateway(config.getServerHost(), config.getServerPort());

                NotificationService notificationService =
                        new NotificationService(config.getUdpPort(), view);

                InputProvider inputProvider = new ConsoleInputProvider()

        ) {

            view.showMessage(
                    "Connected to server: " + config.getServerHost() +
                            ":" + config.getServerPort());

            notificationService.start();
            int actualUdpPort = notificationService.getLocalPort();
            view.showMessage("Notification service active on port " + actualUdpPort);


            GameEngine engine = new GameEngine(gateway, actualUdpPort, view, inputProvider);

            engine.run();


        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        } catch (Exception e) {
            view.showError("Unexpected error: " + e.getMessage());
        } finally {
            view.showMessage("Engine terminated.");
        }
    }
}
