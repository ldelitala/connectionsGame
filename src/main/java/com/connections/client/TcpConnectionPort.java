package com.connections.client;

import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Handles the low-level TCP connection and JSON communication with the server.
 * Acting as the "port" for all client-server requests and responses.
 */
public class TcpConnectionPort implements AutoCloseable {

    // --- Error Messages ---
    private static final String ERR_SERVER_CLOSED = "The server closed the connection.";

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson gson = new Gson();

    /**
     * Constructs a new TcpConnectionPort and establishes a socket connection.
     * 
     * @param host the server host address.
     * @param port the server TCP port.
     * @throws UnknownHostException if the host address could not be resolved.
     * @throws IOException if an I/O error occurs when creating the socket or streams.
     */
    public TcpConnectionPort(String host, int port) throws UnknownHostException, IOException {
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
     * Sends a request to the server and waits for the corresponding response.
     * 
     * @param request the ClientRequest object to send.
     * @return the ServerResponse object received from the server.
     * @throws IOException if communication fails or the server closes the connection.
     */
    public ServerResponse<?> sendRequest(ClientRequest request) throws IOException {
        String json = gson.toJson(request);
        out.println(json);
        String responseJson = in.readLine();
        if (responseJson == null) {
            throw new IOException(ERR_SERVER_CLOSED);
        }
        return gson.fromJson(responseJson, ServerResponse.class);
    }

    /**
     * Closes the underlying socket and associated streams.
     * 
     * @throws IOException if an error occurs during closure.
     */
    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }
}
