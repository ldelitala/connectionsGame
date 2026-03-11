package com.connections.client;

import com.connections.client.view.ClientView;
import com.connections.client.view.ConsoleView;
import java.io.IOException;
import java.util.Scanner;

/**
 * Main entry point for the MVC Client.
 */
public class ClientMain {

    public static void main(String[] args) {
        ClientConfig config = (args.length > 0) ? new ClientConfig(args[0]) : new ClientConfig();
        ClientView view = new ConsoleView();

        view.showMessage("Welcome to Connections Game!");

        try (Scanner scanner = new Scanner(System.in);
                TcpConnectionPort tcpPort = new TcpConnectionPort(config.getServerHost(), config.getServerPort());
                UdpNotificationListener udpListener = new UdpNotificationListener(config.getUdpPort(), view)) {

            view.showMessage("Connected to server: " + config.getServerHost() + ":" + config.getServerPort());

            udpListener.start();
            int actualUdpPort = udpListener.getLocalPort();
            view.showMessage("UDP Notification listener started on port " + actualUdpPort);

            Controller controller = new Controller(tcpPort, scanner, actualUdpPort, view);
            controller.start();

        } catch (IOException e) {
            view.showError("Network error: " + e.getMessage());
        } catch (Exception e) {
            view.showError("Unexpected error: " + e.getMessage());
        } finally {
            view.showMessage("Application terminated.");
        }
    }
}
