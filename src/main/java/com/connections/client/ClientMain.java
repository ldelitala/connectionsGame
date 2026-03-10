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

        System.out.println("Configurazione...");
        ClientConfig config = (args.length > 0) ? new ClientConfig(args[0]) : new ClientConfig();

        System.out.println("Benvenuto in Connections Game!");

        try (Scanner scanner = new Scanner(System.in);
                TcpConnectionPort tcpPort =
                        new TcpConnectionPort(config.getServerHost(), config.getServerPort());
                UdpNotificationListener udpListener =
                        new UdpNotificationListener(config.getUdpPort())) {

            System.out.println("Connesso al server: " + config.getServerHost() + ":" + config.getServerPort());

            udpListener.start();
            int actualUdpPort = udpListener.getLocalPort();
            System.out.println("Listener notifiche UDP avviato sulla porta " + actualUdpPort);

            Controller controller = new Controller(tcpPort, scanner, actualUdpPort);
            controller.start();

        } catch (SocketException e) {
            System.err.println("Errore di connessione UDP: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Errore di comunicazione: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Errore inaspettato: " + e.getMessage());
        } finally {
            System.out.println("Chiusura delle connessioni...");
            System.out.println("Applicazione terminata.");
        }
    }
}
