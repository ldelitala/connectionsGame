package com.connections.client;

import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

/**
 * Main entry point for the CLI client. Orchestrates configuration, network setup, and starts the
 * command controller.
 */
public class ClientMain {

    /**
     * Entry point of the client application.
     * 
     * @param args command line arguments (optional: config path)
     */
    public static void main(String[] args) {

        System.out.println("Configuring...");
        ClientConfig config = (args.length > 0) ? new ClientConfig(args[0]) : new ClientConfig();

        System.out.println("Welcome to Connections Game!");

        try (Scanner scanner = new Scanner(System.in);
                TcpConnectionPort tcpPort =
                        new TcpConnectionPort(config.getServerHost(), config.getServerPort());
                UdpNotificationListener udpListener =
                        new UdpNotificationListener(config.getUdpPort())) {

            System.out.println("Connected to server: " + config.getServerHost() + ":" + config.getServerPort());

            udpListener.start();
            int actualUdpPort = udpListener.getLocalPort();
            System.out.println("UDP Notification listener started on port " + actualUdpPort);

            Controller controller = new Controller(tcpPort, scanner, actualUdpPort);
            controller.start();

        } catch (SocketException e) {
            System.err.println("UDP Connection error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        } finally {
            System.out.println("Closing connections...");
            System.out.println("Application terminated.");
        }
    }
}
