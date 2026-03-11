package com.connections.client.ui.console;

import com.connections.client.ui.InputProvider;
import java.util.Scanner;

/**
 * CLI implementation of InputProvider. Self-manages its own Scanner for System.in.
 */
public class ConsoleInputProvider implements InputProvider {

    private final Scanner scanner;
    private volatile boolean running = true;

    /**
     * Constructs a new ConsoleInputProvider and opens System.in via Scanner.
     */
    public ConsoleInputProvider() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void startListening(InputListener listener) {
        while (running && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                listener.onInput(line);
            }
        }
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public void close() {
        stop();
        scanner.close();
    }
}
