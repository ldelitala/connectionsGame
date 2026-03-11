package com.connections.client.network;

import com.connections.common.protocol.client_requests.ClientRequest;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Handles the low-level TCP connection and raw JSON communication with the server.
 * Acting as a simple "postman", it sends request objects as JSON and returns raw response strings.
 */
public class NetworkGateway implements AutoCloseable {

    private static final String ERR_SERVER_CLOSED = "The server closed the connection.";

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson gson = new Gson();

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
     * Sends a request and returns the raw JSON response string.
     * The caller (Adapter) is responsible for parsing this string into the correct DTO.
     *
     * @param request The request object to send.
     * @return The raw JSON response from the server.
     * @throws IOException If the connection is closed or a network error occurs.
     */
    public String sendRequest(ClientRequest request) throws IOException {
        String json = gson.toJson(request);
        out.println(json);
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
