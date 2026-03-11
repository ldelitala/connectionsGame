package com.connections.client.ui;

/**
 * Interface for a source of user inputs.
 * Acting as an Adapter between external events (CLI, GUI) and the Game Engine.
 */
public interface InputProvider extends AutoCloseable {
    /**
     * Starts listening for inputs.
     * 
     * @param listener the GameEngine or component that will process the normalized commands.
     */
    void startListening(InputListener listener);
    
    /** Stops the input listener. */
    void stop();

    @Override
    void close();

    /**
     * Listener interface for normalized game commands.
     */
    interface InputListener {
        /**
         * Called when a new command is ready.
         * 
         * @param data A space-separated string representing a game command (e.g., "guess w1 w2 w3 w4").
         */
        void onInput(String data);
    }
}
