package it.unipi.connections.server.persistence;

import java.io.*;

/**
 * Persists the server's session state (e.g., the current puzzle rotation index).
 */
public class ServerStateDAO {
    private final String filePath;

    public ServerStateDAO(String filePath) {
        this.filePath = filePath;
        JsonPersistence.ensureDirectory(filePath);
    }

    public synchronized void saveNextPuzzleIndex(int nextIndex) {
        try (Writer writer = new FileWriter(filePath)) {
            JsonPersistence.GSON.toJson(new State(nextIndex), writer);
        } catch (IOException e) {
            System.err.println("DAO Error: Failed to save session state.");
        }
    }

    public synchronized int loadNextPuzzleIndex() {
        if (!JsonPersistence.isReadable(filePath)) return 0;
        try (Reader reader = new FileReader(filePath)) {
            State s = JsonPersistence.GSON.fromJson(reader, State.class);
            return s != null ? s.nextPuzzleIndex : 0;
        } catch (IOException e) {
            System.err.println("DAO Error: Failed to load session state.");
            return 0;
        }
    }

    private static class State {
        int nextPuzzleIndex;
        State(int idx) { this.nextPuzzleIndex = idx; }
    }
}
