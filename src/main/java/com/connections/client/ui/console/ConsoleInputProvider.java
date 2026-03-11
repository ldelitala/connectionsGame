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

    /**
     * Starts a blocking loop that reads lines from System.in using a {@link Scanner}.
     * Normalized commands are sent to the {@link InputListener}.
     * 
     * @param listener The listener that will process the user commands.
     */
    @Override
    public void startListening(InputListener listener) {
        while (running && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                listener.onInput(line);
            }
        }
    }

    /**
     * Sets the running flag to false, which will cause the listening loop to terminate
     * after the next input operation or if interrupted.
     */
    @Override
    public void stop() {
        this.running = false;
    }

    /**
     * Closes the underlying Scanner and releases system resources.
     */
    @Override
    public void close() {
        stop();
        scanner.close();
    }
}
