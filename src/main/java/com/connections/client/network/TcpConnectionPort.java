package com.connections.client.network;

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
 */
public class TcpConnectionPort implements AutoCloseable {

    private static final String ERR_SERVER_CLOSED = "The server closed the connection.";

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson gson = new Gson();

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

    public ServerResponse<?> sendRequest(ClientRequest request) throws IOException {
        String json = gson.toJson(request);
        out.println(json);
        String responseJson = in.readLine();
        if (responseJson == null) {
            throw new IOException(ERR_SERVER_CLOSED);
        }
        return gson.fromJson(responseJson, ServerResponse.class);
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }
}
