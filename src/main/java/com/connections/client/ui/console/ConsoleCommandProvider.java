package com.connections.client.ui.console;

import com.connections.client.ui.CommandProvider;
import java.util.Scanner;

/**
 * CLI implementation of CommandProvider. Self-manages its own Scanner for System.in.
 */
public class ConsoleCommandProvider implements CommandProvider {

    private final Scanner scanner;
    private volatile boolean running = true;

    /**
     * Constructs a new ConsoleCommandProvider and opens System.in via Scanner.
     */
    public ConsoleCommandProvider() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void startProviding(CommandListener listener) {
        // while the provider is active and has next lines...
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

    /**
     * Closes the underlying scanner and System.in.
     */
    @Override
    public void close() {
        stop();
        scanner.close();
    }
}
