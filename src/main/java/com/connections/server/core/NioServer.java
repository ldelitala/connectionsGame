package com.connections.server.core;

import com.connections.server.handler.RequestDispatcher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

/**
 * High-performance NIO TCP Server.
 * Handles connections and raw I/O, delegating logic to a RequestDispatcher.
 */
public class NioServer {
    private static final int BUFFER_SIZE = 8192;

    private final int port;
    private final RequestDispatcher dispatcher;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = true;

    public NioServer(int port, RequestDispatcher dispatcher) {
        this.port = port;
        this.dispatcher = dispatcher;
    }

    public void start() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("TCP Server started on port " + port);

        while (running) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                if (!key.isValid()) continue;

                try {
                    if (key.isAcceptable()) {
                        handleAccept();
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                } catch (IOException e) {
                    closeConnection(key);
                }
            }
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, new ClientSession());
        System.out.println("Accepted connection from: " + clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int bytesRead = channel.read(buffer);

        if (bytesRead == -1) {
            closeConnection(key);
            return;
        }

        buffer.flip();
        String data = StandardCharsets.UTF_8.decode(buffer).toString();
        ClientSession session = (ClientSession) key.attachment();
        session.appendInput(data);

        String message;
        while ((message = session.extractMessage()) != null) {
            // Offload processing to dispatcher (Thread Pool)
            dispatcher.dispatch(message, session, () -> {
                // Callback once response is ready: wake up selector to process write
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                selector.wakeup();
            });
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientSession session = (ClientSession) key.attachment();
        Queue<ByteBuffer> queue = session.getOutputQueue();

        while (!queue.isEmpty()) {
            ByteBuffer buffer = queue.peek();
            channel.write(buffer);
            if (buffer.hasRemaining()) {
                // Socket buffer full, stay interested in OP_WRITE and try again later
                return;
            }
            queue.poll(); // Finished writing this buffer
        }

        // All pending data sent, stop being interested in OP_WRITE
        key.interestOps(SelectionKey.OP_READ);
    }

    private void closeConnection(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            System.out.println("Closing connection: " + channel.getRemoteAddress());
            
            ClientSession session = (ClientSession) key.attachment();
            if (session != null && session.getUsername() != null) {
                dispatcher.handleDisconnect(session.getUsername());
            }
            
            key.cancel();
            channel.close();
        } catch (IOException ignored) {}
    }

    public void stop() {
        running = false;
        selector.wakeup();
    }
}
