package com.connections.client;

import com.connections.client.network.NotificationService;
import com.connections.client.network.NetworkGateway;
import com.connections.client.controller.GameEngine;
import com.connections.client.ui.*;
import com.connections.client.ui.console.*;
import java.io.IOException;

/**
 * Main entry point for the 100% Modular MVC Game Client.
 */
public class ClientMain {

    public static void main(String[] args) {
        ClientConfig config = (args.length > 0) ? new ClientConfig(args[0]) : new ClientConfig();
        
        // 1. Setup the UI layer
        ClientView view = new ConsoleView();

        view.showMessage("Welcome to Connections Engine!");

        // try-with-resources manages the lifecycle of the network and the input provider.
        try (NetworkGateway gateway = new NetworkGateway(config.getServerHost(), config.getServerPort());
                NotificationService notificationService = new NotificationService(config.getUdpPort(), view);
                CommandProvider inputProvider = new ConsoleCommandProvider()) {

            view.showMessage("Connected to server: " + config.getServerHost() + ":" + config.getServerPort());

            notificationService.start();
            int actualUdpPort = notificationService.getLocalPort();
            view.showMessage("Notification service active on port " + actualUdpPort);

            // 2. Setup the Game Engine (Connects Network, Input, and View)
            GameEngine engine = new GameEngine(gateway, actualUdpPort, view, inputProvider);
            
            // 3. Run the Engine (Blocking call until exit)
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
