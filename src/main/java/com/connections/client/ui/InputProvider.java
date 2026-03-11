package com.connections.client.ui;

/**
 * Interface for a source of user inputs.
 * Acting as an Adapter between external events (CLI, GUI) and the Game Engine.
 */
public interface InputProvider extends AutoCloseable {
    /**
     * Starts listening for user inputs in a blocking or non-blocking way.
     * 
     * @param listener The component that will process the received command lines.
     */
    void startListening(InputListener listener);
    
    /** 
     * Signals the input provider to stop listening and terminate any background threads. 
     */
    void stop();

    /**
     * Releases any resources (e.g., System.in, Scanner) held by the provider.
     */
    @Override
    void close();

    /**
     * Functional interface for receiving processed user input.
     */
    interface InputListener {
        /**
         * Invoked when a new command line is ready for processing.
         * 
         * @param data The raw or trimmed command string.
         */
        void onInput(String data);
    }
}
