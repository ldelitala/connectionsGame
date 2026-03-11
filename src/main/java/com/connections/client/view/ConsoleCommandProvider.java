package com.connections.client.view;

import java.util.Scanner;

/**
 * CLI implementation of CommandProvider. Uses Scanner to read from System.in.
 */
public class ConsoleCommandProvider implements CommandProvider {

    private final Scanner scanner;
    private volatile boolean running = true;

    public ConsoleCommandProvider(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public void startProviding(CommandListener listener) {
        while (running && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                listener.onCommand(line);
            }
        }
    }

    @Override
    public void stop() {
        this.running = false;
    }
}
