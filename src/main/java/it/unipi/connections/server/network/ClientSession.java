package it.unipi.connections.server.network;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages the state and I/O buffers for a specific client connection.
 * Acts as an attachment for NIO SelectionKeys.
 */
public class ClientSession {
    private final StringBuilder inputBuffer = new StringBuilder();
    private final Queue<ByteBuffer> outputQueue = new ConcurrentLinkedQueue<>();
    
    // Identity state
    private String username;

    /**
     * Appends received bytes to the session's input buffer.
     */
    public void appendInput(String data) {
        inputBuffer.append(data);
    }

    /**
     * Checks if a complete JSON message (terminated by \n) is present in the buffer.
     * @return the message string if complete, null otherwise.
     */
    public String extractMessage() {
        int index = inputBuffer.indexOf("\n");
        if (index != -1) {
            String message = inputBuffer.substring(0, index).trim();
            inputBuffer.delete(0, index + 1);
            return message;
        }
        return null;
    }

    /**
     * Enqueues a response buffer to be sent to the client.
     */
    public void enqueueOutput(ByteBuffer buffer) {
        outputQueue.offer(buffer);
    }

    public Queue<ByteBuffer> getOutputQueue() {
        return outputQueue;
    }

    public boolean hasPendingOutput() {
        return !outputQueue.isEmpty();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
