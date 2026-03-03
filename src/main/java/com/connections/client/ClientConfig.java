package com.connections.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration for the Connections Client.
 */
public class ClientConfig {
    
    private final Properties props = new Properties();
    private final File configFile;

    private static final String DEFAULT_CONFIG_PATH = "config/client.properties";

    // Keys
    private static final String KEY_SERVER_HOST = "client.server_host";
    private static final String KEY_SERVER_PORT = "client.server_port";

    public ClientConfig() {
        this.configFile = new File(DEFAULT_CONFIG_PATH);
        setDefaultProperties();
        loadOrInitConfig();
    }

    private void setDefaultProperties() {
        props.setProperty(KEY_SERVER_HOST, "localhost");
        props.setProperty(KEY_SERVER_PORT, "8080");
    }

    private void loadOrInitConfig() {        
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
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
        } catch (IOException e) {
            System.err.println("Could not save client config: " + e.getMessage());
        }
    }

    public String getServerHost() { return props.getProperty(KEY_SERVER_HOST); }
    public int getServerPort() { return Integer.parseInt(props.getProperty(KEY_SERVER_PORT)); }
}
