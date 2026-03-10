package com.connections.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration for the Connections Server.
 */
public class ServerConfig {
    private final Properties props = new Properties();
    private final File configFile;

    private static final String DEFAULT_CONFIG_PATH = "config/server.properties";

    // Keys
    private static final String KEY_TCP_PORT = "server.tcp_port";
    private static final String KEY_UDP_PORT = "server.udp_port";
    private static final String KEY_THREAD_POOL = "server.thread_pool_size";
    private static final String KEY_GAME_DATA = "server.game_data_path";
    private static final String KEY_USER_DATA = "server.user_data_path";
    private static final String KEY_LOG_PATH = "server.log_path";
    private static final String KEY_GAME_DURATION = "server.game_duration_ms";

    public ServerConfig() {
        this(DEFAULT_CONFIG_PATH);
    }

    public ServerConfig(String configPath) {
        this.configFile = new File(configPath);
        setDefaultProperties();
        loadOrInitConfig();
    }

    private void setDefaultProperties() {
        props.setProperty(KEY_TCP_PORT, "8080");
        props.setProperty(KEY_UDP_PORT, "8081");
        props.setProperty(KEY_THREAD_POOL, "10");
        props.setProperty(KEY_GAME_DATA, "Connections_Data.json");
        props.setProperty(KEY_USER_DATA, "data/users.json");
        props.setProperty(KEY_LOG_PATH, "data/games_log.json");
        props.setProperty(KEY_GAME_DURATION, "300000"); // 5 minutes
    }

    private void loadOrInitConfig() {
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
                System.out.println("Config loaded from: " + configFile.getPath());
            } catch (IOException e) {
                System.err.println("Error reading server config: " + e.getMessage());
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
            props.store(out, "Connections Server Configuration");
            System.out.println("Default config file created: " + configFile.getPath());
        } catch (IOException e) {
            System.err.println("Could not save server config: " + e.getMessage());
        }
    }

    // Getters
    public int getTcpPort() { return Integer.parseInt(props.getProperty(KEY_TCP_PORT)); }
    public int getUdpPort() { return Integer.parseInt(props.getProperty(KEY_UDP_PORT)); }
    public int getThreadPoolSize() { return Integer.parseInt(props.getProperty(KEY_THREAD_POOL)); }
    public String getGameDataPath() { return props.getProperty(KEY_GAME_DATA); }
    public String getUserDataPath() { return props.getProperty(KEY_USER_DATA); }
    public String getLogPath() { return props.getProperty(KEY_LOG_PATH); }
    public long getGameDurationMillis() { return Long.parseLong(props.getProperty(KEY_GAME_DURATION)); }
}
