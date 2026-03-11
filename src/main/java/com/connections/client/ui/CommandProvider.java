package com.connections.client.ui;

/**
 * Interface for a source of user commands.
 * This can be a CLI loop with Scanner, or a GUI button event.
 */
public interface CommandProvider extends AutoCloseable {
    /**
     * Starts providing commands to the listener.
     */
    void startProviding(CommandListener listener);
    
    /** Stops providing commands. */
    void stop();

    @Override
    void close();

    /** Listener interface to receive commands from the provider. */
    interface CommandListener {
        void onCommand(String line);
    }
}
