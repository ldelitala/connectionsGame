package it.unipi.connections.client.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * Simplified NIO NetworkGateway for synchronous request-response communication.
 */
public class NetworkGateway implements AutoCloseable {

    private static final String ERR_SERVER_CLOSED = "The server closed the connection.";
    private static final int BUFFER_SIZE = 4096;

    private final SocketChannel socketChannel;
    private final ByteBuffer buffer;

    public NetworkGateway(String host, int port) throws IOException {
        this.socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
        this.socketChannel.configureBlocking(true);
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    /**
     * Sends a JSON request and waits for the response.
     */
    public String sendRequest(String jsonRequest) throws IOException {
        send(jsonRequest);
        return receiveResponse();
    }

    private void send(String jsonRequest) throws IOException {
        String payload = jsonRequest + "\n";
        byte[] message = payload.getBytes(StandardCharsets.UTF_8);

        buffer.clear();
        buffer.put(message);
        buffer.flip();

        while (buffer.hasRemaining()) {
            socketChannel.write(buffer);
        }
    }

    private String receiveResponse() throws IOException {
        ByteArrayOutputStream lineCollector = new ByteArrayOutputStream();

        while (true) {
            buffer.clear();
            int bytesRead = socketChannel.read(buffer);

            if (bytesRead == -1) {
                // If we reach EOF without a newline, the response is invalid/incomplete
                throw new IOException(ERR_SERVER_CLOSED + " (Incomplete response)");
            }

            buffer.flip();
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                if (b == '\n') {
                    return new String(lineCollector.toByteArray(), StandardCharsets.UTF_8).trim();
                }
                if (b != '\r') {
                    lineCollector.write(b);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (socketChannel != null && socketChannel.isOpen()) {
            socketChannel.close();
        }
    }
}
