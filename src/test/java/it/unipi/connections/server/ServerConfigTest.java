package it.unipi.connections.server;

import it.unipi.connections.server.ServerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ServerConfigTest {

    @TempDir
    Path tempDir;

    @Test
    public void testAutoCreation() {
        Path propsFile = tempDir.resolve("auto_config.properties");
        File file = propsFile.toFile();
        assertFalse(file.exists());

        ServerConfig config = new ServerConfig(propsFile.toString());
        assertTrue(file.exists(), "Config file should be created automatically");
        assertEquals(8080, config.getTcpPort(), "Should use default port");
    }

    @Test
    public void testLoadingProperties() throws IOException {
        Path propsFile = tempDir.resolve("manual_config.properties");
        try (FileWriter writer = new FileWriter(propsFile.toFile())) {
            writer.write("server.tcp_port=9999\n");
            writer.write("server.thread_pool_size=5\n");
        }

        ServerConfig config = new ServerConfig(propsFile.toString());
        assertEquals(9999, config.getTcpPort());
        assertEquals(5, config.getThreadPoolSize());
        // Check a default that wasn't overridden
        assertEquals("Connections_Data.json", config.getPuzzleDataPath());
    }
}
