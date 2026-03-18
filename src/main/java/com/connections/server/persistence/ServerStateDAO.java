package com.connections.server.persistence;

import com.google.gson.Gson;
import java.io.*;

/**
 * Persists the server's session state (e.g., the current puzzle rotation index).
 * This ensures consistency after a server restart.
 */
public class ServerStateDAO {
    private final String filePath;
    private final Gson gson = new Gson();

    public ServerStateDAO(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Persists the index of the next puzzle to be served.
     * 
     * @param nextIndex the 0-based index of the puzzle.
     */
    public synchronized void saveNextPuzzleIndex(int nextIndex) {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (Writer writer = new FileWriter(file)) {
            State state = new State(nextIndex);
            gson.toJson(state, writer);
        } catch (IOException e) {
            System.err.println("Error saving server session state: " + e.getMessage());
        }
    }

    /**
     * Loads the last saved puzzle rotation index.
     * 
     * @return the saved index, or 0 if none is found.
     */
    public synchronized int loadNextPuzzleIndex() {
        File file = new File(filePath);
        if (!file.exists()) return 0;
        
        try (Reader reader = new FileReader(file)) {
            State state = gson.fromJson(reader, State.class);
            return state != null ? state.nextPuzzleIndex : 0;
        } catch (IOException e) {
            System.err.println("Error loading server session state: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Simple container for Gson serialization of server session state.
     */
    private static class State {
        int nextPuzzleIndex;
        State(int index) { this.nextPuzzleIndex = index; }
    }
}
