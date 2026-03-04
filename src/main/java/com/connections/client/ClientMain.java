package com.connections.client;

import java.io.IOException;
import java.util.Scanner;

/**
 * Main entry point for the CLI client.
 * Orchestrates configuration, network setup, and starts the command controller.
 */
public class ClientMain {
    
    public static void main(String[] args) {
        ClientConfig config = new ClientConfig();
        Scanner scanner = new Scanner(System.in);
        ConnectionClient tcpClient = null;
        UdpNotificationListener udpListener = null;

        System.out.println("Benvenuto in Connections Game!");

        try {
            // 1. Configurazione connessione TCP
            tcpClient = new ConnectionClient(config.getServerHost(), config.getServerPort());
            System.out.println("Connesso al server: " + config.getServerHost() + ":" + config.getServerPort());

            // 2. Configurazione listener UDP (per notifiche asincrone)
            // Se nel file config la porta è 0, il sistema ne assegnerà una libera
            int udpPort = config.getUdpPort(); 
            udpListener = new UdpNotificationListener(udpPort);
            new Thread(udpListener).start();
            
            // Ottengo la porta effettiva (utile se era 0)
            int actualUdpPort = udpListener.getLocalPort();
            System.out.println("Listener notifiche UDP avviato sulla porta " + actualUdpPort);

            // 3. Avvio del ClientController (Coordinatore)
            ClientController controller = new ClientController(tcpClient, scanner, actualUdpPort);
            
            // Il programma entra nel loop dei comandi finché non viene chiamato 'exit'
            controller.start(scanner);

        } catch (IOException e) {
            System.err.println("Errore fatale all'avvio: " + e.getMessage());
        } finally {
            // Pulizia finale al ritorno dal controller (comando exit)
            System.out.println("Chiusura delle connessioni...");
            try {
                if (tcpClient != null) tcpClient.close();
                if (udpListener != null) udpListener.stop();
            } catch (IOException ignored) {}
            scanner.close();
            System.out.println("Applicazione terminata.");
        }
    }
}
