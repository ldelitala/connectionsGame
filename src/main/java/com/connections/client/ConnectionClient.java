package com.connections.client;

import com.connections.common.requests.Request;
import com.connections.common.responses.Response;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles the low-level TCP connection and JSON communication with the server.
 */
public class ConnectionClient implements AutoCloseable {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson gson = new Gson();

    public ConnectionClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public Response sendRequest(Request request) throws IOException {
        String json = gson.toJson(request);
        out.println(json);
        String responseJson = in.readLine();
        if (responseJson == null) {
            throw new IOException("Server closed connection.");
        }
        return gson.fromJson(responseJson, Response.class);
    }

    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}
