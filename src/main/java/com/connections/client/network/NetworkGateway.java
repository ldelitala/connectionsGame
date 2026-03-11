package com.connections.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Handles the low-level TCP connection.
 * Acts as a raw string tunnel: sends a JSON string and returns a JSON string.
 */
public class NetworkGateway implements AutoCloseable {

    private static final String ERR_SERVER_CLOSED = "The server closed the connection.";

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    /**
     * Creates a new TCP connection to the specified server.
     */
    public NetworkGateway(String host, int port) throws UnknownHostException, IOException {
        this.socket = new Socket(host, port);
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    /**
     * Sends a raw JSON string and returns the raw JSON response from the server.
     *
     * @param jsonRequest The JSON string to send.
     * @return The raw JSON response string.
     * @throws IOException If the connection is closed or a network error occurs.
     */
    public String sendRequest(String jsonRequest) throws IOException {
        out.println(jsonRequest);
        String responseJson = in.readLine();
        if (responseJson == null) {
            throw new IOException(ERR_SERVER_CLOSED);
        }
        return responseJson;
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }
}
