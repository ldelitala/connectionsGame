package com.connections.client;

import com.connections.client.network.UdpNotificationListener;
import com.connections.client.network.TcpConnectionPort;
import com.connections.client.controller.Controller;
import com.connections.client.ui.*;
import com.connections.client.ui.console.*;
import java.io.IOException;
import java.util.Scanner;

/**
 * Main entry point for the 100% Modular MVC Client.
 */
public class ClientMain {

    public static void main(String[] args) {
        ClientConfig config = (args.length > 0) ? new ClientConfig(args[0]) : new ClientConfig();
        
        // 1. Setup the VIEW (Modular)
        ClientView view = new ConsoleView();

        view.showMessage("Welcome to Connections Puzzle!");

        try (Scanner scanner = new Scanner(System.in);
                TcpConnectionPort tcpPort = new TcpConnectionPort(config.getServerHost(), config.getServerPort());
                UdpNotificationListener udpListener = new UdpNotificationListener(config.getUdpPort(), view)) {

            view.showMessage("Connected to server: " + config.getServerHost() + ":" + config.getServerPort());

            udpListener.start();
            int actualUdpPort = udpListener.getLocalPort();
            view.showMessage("UDP Notification listener started on port " + actualUdpPort);

            // 2. Setup the INPUT (Modular)
            CommandProvider inputProvider = new ConsoleCommandProvider(scanner);

            // 3. Setup the CONTROLLER (The Brain, connects Input and View)
            Controller controller = new Controller(tcpPort, actualUdpPort, view, inputProvider);
            
            // 4. Start the engine
            controller.run();

        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        } catch (Exception e) {
            view.showError("Unexpected error: " + e.getMessage());
        } finally {
            view.showMessage("Application terminated.");
        }
    }
}
