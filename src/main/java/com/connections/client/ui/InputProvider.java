package com.connections.client.ui;

/**
 * Interface for a source of user inputs.
 * This can be a CLI loop with Scanner, or a GUI button event.
 */
public interface InputProvider extends AutoCloseable {
    /**
     * Starts listening for inputs and sends them to the listener.
     */
    void startListening(InputListener listener);
    
    /** Stops the input listener. */
    void stop();

    @Override
    void close();

    /** Listener interface to receive inputs from the provider. */
    interface InputListener {
        void onInput(String data);
    }
}
