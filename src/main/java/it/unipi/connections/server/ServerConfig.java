package it.unipi.connections.server;

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

    // Default Values
    private static final String DEFAULT_TCP_PORT = "8080";
    private static final String DEFAULT_UDP_PORT = "8081";
    private static final String DEFAULT_THREAD_POOL_SIZE = "10";
    private static final String DEFAULT_PUZZLE_DATA_PATH = "Connections_Data.json";
    private static final String DEFAULT_USER_DATA_PATH = "data/users.json";
    private static final String DEFAULT_LOG_PATH = "data/games_log.json";
    private static final String DEFAULT_STATE_PATH = "data/server_state.json";
    private static final String DEFAULT_PUZZLE_DURATION_MS = "300000"; // 5 minutes
    private static final String DEFAULT_PUZZLE_DELAY_MS = "10000"; // 10 seconds

    // Keys
    private static final String KEY_TCP_PORT = "server.tcp_port";
    private static final String KEY_UDP_PORT = "server.udp_port";
    private static final String KEY_THREAD_POOL = "server.thread_pool_size";
    private static final String KEY_PUZZLE_DATA = "server.game_data_path";
    private static final String KEY_USER_DATA = "server.user_data_path";
    private static final String KEY_LOG_PATH = "server.log_path";
    private static final String KEY_STATE_PATH = "server.state_path";
    private static final String KEY_PUZZLE_DURATION = "server.game_duration_ms";
    private static final String KEY_PUZZLE_DELAY = "server.game_delay_ms";

    public ServerConfig() {
        this(DEFAULT_CONFIG_PATH);
    }

    public ServerConfig(String configPath) {
        this.configFile = new File(configPath);
        setDefaultProperties();
        loadOrInitConfig();
    }

    private void setDefaultProperties() {
        props.setProperty(KEY_TCP_PORT, DEFAULT_TCP_PORT);
        props.setProperty(KEY_UDP_PORT, DEFAULT_UDP_PORT);
        props.setProperty(KEY_THREAD_POOL, DEFAULT_THREAD_POOL_SIZE);
        props.setProperty(KEY_PUZZLE_DATA, DEFAULT_PUZZLE_DATA_PATH);
        props.setProperty(KEY_USER_DATA, DEFAULT_USER_DATA_PATH);
        props.setProperty(KEY_LOG_PATH, DEFAULT_LOG_PATH);
        props.setProperty(KEY_STATE_PATH, DEFAULT_STATE_PATH);
        props.setProperty(KEY_PUZZLE_DURATION, DEFAULT_PUZZLE_DURATION_MS);
        props.setProperty(KEY_PUZZLE_DELAY, DEFAULT_PUZZLE_DELAY_MS);
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
    public int getTcpPort() {
        return Integer.parseInt(props.getProperty(KEY_TCP_PORT));
    }

    public int getUdpPort() {
        return Integer.parseInt(props.getProperty(KEY_UDP_PORT));
    }

    public int getThreadPoolSize() {
        return Integer.parseInt(props.getProperty(KEY_THREAD_POOL));
    }

    public String getPuzzleDataPath() {
        return props.getProperty(KEY_PUZZLE_DATA);
    }

    public String getUserDataPath() {
        return props.getProperty(KEY_USER_DATA);
    }

    public String getLogPath() {
        return props.getProperty(KEY_LOG_PATH);
    }

    public String getStatePath() {
        return props.getProperty(KEY_STATE_PATH);
    }

    public long getPuzzleDurationMillis() {
        return Long.parseLong(props.getProperty(KEY_PUZZLE_DURATION));
    }

    public long getPuzzleDelayMillis() {
        return Long.parseLong(props.getProperty(KEY_PUZZLE_DELAY));
    }
}
