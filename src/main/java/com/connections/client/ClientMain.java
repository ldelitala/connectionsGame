package com.connections.client;

import java.io.IOException;
import java.util.Scanner;

/**
 * Main entry point for the CLI client. Orchestrates configuration, network setup, and starts the
 * command controller.
 */
public class ClientMain {

    public static void main(String[] args) {
        ClientConfig config = new ClientConfig();
        Scanner scanner = new Scanner(System.in);
        TcpConnectionPort tcpPort = null;
        UdpNotificationListener udpListener = null;

        System.out.println("Benvenuto in Connections Game!");

        try {
            // 1. Configurazione connessione TCP tramite il "Porto"
            tcpPort = new TcpConnectionPort(config.getServerHost(), config.getServerPort());
            System.out.println(
                    "Connesso al server: " + config.getServerHost() + ":" + config.getServerPort());

            // 2. Configurazione listener UDP
            int udpPort = config.getUdpPort();
            udpListener = new UdpNotificationListener(udpPort);
            new Thread(udpListener).start();

            int actualUdpPort = udpListener.getLocalPort();
            System.out.println("Listener notifiche UDP avviato sulla porta " + actualUdpPort);

            // 3. Avvio del ClientController passando il "Porto"
            ClientController controller = new ClientController(tcpPort, scanner, actualUdpPort);
            controller.start(scanner);

        } catch (IOException e) {
            System.err.println("Errore fatale all'avvio: " + e.getMessage());
        } finally {
            // Pulizia finale
            System.out.println("Chiusura delle connessioni...");
            try {
                if (tcpPort != null)
                    tcpPort.close();
                if (udpListener != null)
                    udpListener.stopListener(); // Use the correct method name
            } catch (IOException ignored) {
            }
            scanner.close();
            System.out.println("Applicazione terminata.");
        }
    }
}
