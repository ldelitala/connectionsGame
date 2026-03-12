package com.connections.server;

import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;
import com.connections.server.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance NIO TCP Server for the Connections puzzle.
 * Uses a thread pool to handle request processing while the main thread manages I/O selection.
 */
public class ConnectionsServer {

    // --- Configuration ---
    private static final int BUFFER_SIZE = 4096;
    private static final String CHARSET_NAME = "UTF-8";

    // --- JSON Protocol Keys ---
    private static final String KEY_OPERATION = "operation";

    private final ServerConfig config;
    private final UserManager userManager;
    private final PuzzleSessionManager puzzleSessionManager;
    private final UdpNotifier udpNotifier;
    private final ExecutorService threadPool;
    private final Gson gson = new Gson();

    public ConnectionsServer(ServerConfig config, UserManager userManager, PuzzleSessionManager puzzleSessionManager, UdpNotifier udpNotifier) {
        this.config = config;
        this.userManager = userManager;
        this.puzzleSessionManager = puzzleSessionManager;
        this.udpNotifier = udpNotifier;
        this.threadPool = Executors.newFixedThreadPool(config.getThreadPoolSize());
    }

    public void start() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(config.getTcpPort()));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("TCP Server listening on port " + config.getTcpPort());

        while (!Thread.currentThread().isInterrupted()) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                if (key.isAcceptable()) {
                    acceptConnection(selector, serverChannel);
                } else if (key.isReadable()) {
                    handleRead(key);
                }
                iter.remove();
            }
        }
    }

    private void acceptConnection(Selector selector, ServerSocketChannel serverChannel) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("New connection from: " + clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        try {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                closeConnection(key, channel);
                return;
            }
            buffer.flip();
            String requestJson = new String(buffer.array(), 0, bytesRead, CHARSET_NAME).trim();
            if (requestJson.isEmpty()) return;

            // Offload processing to thread pool
            threadPool.execute(() -> processRequest(key, channel, requestJson));

        } catch (IOException e) {
            System.err.println("Read error: " + e.getMessage());
            closeConnection(key, channel);
        }
    }

    private void processRequest(SelectionKey key, SocketChannel channel, String json) {
        try {
            // 1. Peek operation type to use correct DTO
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String op = obj.get(KEY_OPERATION).getAsString().toLowerCase();

            // 2. Delegate logic to UserManager or PuzzleSessionManager
            // (Simplified for brevity, following the project's logic)
            // ... Logic processing ...

            // 3. Send Response
            sendResponse(channel, ServerResponse.ok("Operation successful", null));

        } catch (Exception e) {
            System.err.println("Processing error: " + e.getMessage());
            sendResponse(channel, ServerResponse.error("Server error: " + e.getMessage()));
        }
    }

    private void sendResponse(SocketChannel channel, ServerResponse<?> response) {
        try {
            String json = gson.toJson(response) + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(CHARSET_NAME));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            System.err.println("Write error: " + e.getMessage());
        }
    }

    private void closeConnection(SelectionKey key, SocketChannel channel) {
        try {
            System.out.println("Closing connection: " + channel.getRemoteAddress());
            key.cancel();
            channel.close();
        } catch (IOException ignored) {}
    }
}
