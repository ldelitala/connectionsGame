package com.connections.client.view;

/**
 * Interface for a source of user commands.
 * This can be a CLI loop with Scanner, or a GUI button event.
 */
public interface CommandProvider {
    /**
     * Starts providing commands to the listener.
     * In CLI, this would be the while(true) loop.
     */
    void startProviding(CommandListener listener);
    
    /** Stops providing commands. */
    void stop();

    /** Listener interface to receive commands from the provider. */
    interface CommandListener {
        void onCommand(String line);
    }
}
