package it.unipi.connections.server.network;

import it.unipi.connections.server.network.bridge.ProtocolBridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance NIO TCP Server.
 * Manages raw network I/O and offloads processing to a worker thread pool.
 */
public class NioServer {
    private static final int BUFFER_SIZE = 8192;

    private final int port;
    private final ProtocolBridge bridge;
    private final ExecutorService workerPool;
    
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = true;

    public NioServer(int port, ProtocolBridge bridge, int threadPoolSize) {
        this.port = port;
        this.bridge = bridge;
        this.workerPool = Executors.newFixedThreadPool(threadPoolSize);
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
        cleanup();
    }

    private void cleanup() {
        try {
            if (selector != null) selector.close();
            if (serverChannel != null) serverChannel.close();
            System.out.println("NioServer resources cleaned up.");
        } catch (IOException e) {
            System.err.println("Error during server cleanup: " + e.getMessage());
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
            final String finalMsg = message;
            // Offload business logic to worker pool
            try {
                workerPool.execute(() -> {
                    ByteBuffer response = bridge.process(finalMsg, session);
                    session.enqueueOutput(response);

                    // Wake up selector to process write (if key is still valid)
                    if (key.isValid()) {
                        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        selector.wakeup();
                    }
                });
            } catch (java.util.concurrent.RejectedExecutionException e) {
                System.err.println("Server is shutting down. Discarding request from " + session.getUsername());
                closeConnection(key);
            }
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
                return;
            }
            queue.poll();
        }

        key.interestOps(SelectionKey.OP_READ);
    }

    private void closeConnection(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            System.out.println("Closing connection: " + channel.getRemoteAddress());
            
            ClientSession session = (ClientSession) key.attachment();
            if (session != null && session.getUsername() != null) {
                bridge.handleDisconnect(session.getUsername());
            }
            
            key.cancel();
            channel.close();
        } catch (IOException ignored) {}
    }

    public void stop() {
        running = false;
        workerPool.shutdown();
        selector.wakeup();
    }
}
