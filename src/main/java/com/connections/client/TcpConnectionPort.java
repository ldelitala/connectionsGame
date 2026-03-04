package com.connections.client;

import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles the low-level TCP connection and JSON communication with the server.
 * Acting as the "port" for all client-server requests and responses.
 */
public class TcpConnectionPort implements AutoCloseable {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson gson = new Gson();

    public TcpConnectionPort(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Sends a request to the server and waits for the corresponding response.
     */
    public ServerResponse sendRequest(ClientRequest request) throws IOException {
        String json = gson.toJson(request);
        out.println(json);
        String responseJson = in.readLine();
        if (responseJson == null) {
            throw new IOException("Il server ha chiuso la connessione.");
        }
        return gson.fromJson(responseJson, ServerResponse.class);
    }

    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}
