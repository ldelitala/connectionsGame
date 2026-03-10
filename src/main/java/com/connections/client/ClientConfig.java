package com.connections.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration for the Connections Client.
 * Follows the "Single Source of Truth" principle, matching the Server's config logic.
 */
public class ClientConfig {

    // --- Config Keys ---
    private static final String KEY_SERVER_HOST = "client.server_host";
    private static final String KEY_SERVER_PORT = "client.server_port";
    private static final String KEY_UDP_PORT = "client.udp_port";

    // --- Defaults ---
    private static final String DEFAULT_CONFIG_PATH = "config/client.properties";
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final String DEFAULT_SERVER_PORT = "8080";
    private static final String DEFAULT_UDP_PORT = "0";

    private final Properties props = new Properties();
    private final File configFile;

    /**
     * Creates a new ClientConfig using the default path.
     */
    public ClientConfig() {
        this(DEFAULT_CONFIG_PATH);
    }

    /**
     * Creates a new ClientConfig using a custom path.
     * @param configPath path to the properties file.
     */
    public ClientConfig(String configPath) {
        this.configFile = new File(configPath);
        setDefaultProperties();
        loadOrInitConfig();
    }

    private void setDefaultProperties() {
        props.setProperty(KEY_SERVER_HOST, DEFAULT_SERVER_HOST);
        props.setProperty(KEY_SERVER_PORT, DEFAULT_SERVER_PORT);
        props.setProperty(KEY_UDP_PORT, DEFAULT_UDP_PORT);
    }

    private void loadOrInitConfig() {
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
                System.out.println("Config loaded from: " + configFile.getPath());
            } catch (IOException e) {
                System.err.println("Error reading client config: " + e.getMessage());
            }
        } else {
            saveConfig();
        }
    }

    private void saveConfig() {
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Connections Client Configuration");
            System.out.println("Default config file created: " + configFile.getPath());
        } catch (IOException e) {
            System.err.println("Could not save client config: " + e.getMessage());
        }
    }

    // --- GETTERS ---
    public String getServerHost() {
        return props.getProperty(KEY_SERVER_HOST);
    }

    public int getServerPort() {
        return Integer.parseInt(props.getProperty(KEY_SERVER_PORT));
    }

    public int getUdpPort() {
        return Integer.parseInt(props.getProperty(KEY_UDP_PORT));
    }
}
