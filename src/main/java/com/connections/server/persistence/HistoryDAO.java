package com.connections.server.persistence;

import com.connections.server.models.PlayerResult;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages persistence for past puzzle results.
 * Each entry is stored as a JSON line for efficient appending and indexing.
 */
public class HistoryDAO {
    private final String filePath;
    private final Map<Integer, Long> puzzleOffsets = new ConcurrentHashMap<>();

    public HistoryDAO(String filePath) {
        this.filePath = filePath;
        JsonPersistence.ensureDirectory(filePath);
        buildIndex();
    }

    private void buildIndex() {
        if (!JsonPersistence.isReadable(filePath)) return;

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long pos = 0;
            String line;
            while ((line = raf.readLine()) != null) {
                if (line.contains("\"puzzleId\":")) {
                    int id = parseId(line);
                    if (id != -1) puzzleOffsets.put(id, pos);
                }
                pos = raf.getFilePointer();
            }
            System.out.println("DAO: Indexed " + puzzleOffsets.size() + " history records.");
        } catch (IOException e) {
            System.err.println("DAO Error: Failed to index history: " + e.getMessage());
        }
    }

    private int parseId(String json) {
        try {
            int start = json.indexOf("\"puzzleId\":") + 11;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) { return -1; }
    }

    public synchronized void archivePuzzle(int puzzleId, Map<String, PlayerResult> results) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            long offset = new File(filePath).length();
            writer.write(JsonPersistence.GSON.toJson(new Record(puzzleId, results)));
            writer.newLine();
            puzzleOffsets.put(puzzleId, offset);
        } catch (IOException e) {
            System.err.println("DAO Error: Failed to archive puzzle " + puzzleId);
        }
    }

    /** Alias for coordinator compatibility. */
    public void savePuzzleResults(int id, Map<String, PlayerResult> res) { archivePuzzle(id, res); }

    public Map<String, PlayerResult> getPuzzleResults(int puzzleId) {
        Long offset = puzzleOffsets.get(puzzleId);
        if (offset == null) return null;

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(offset);
            String line = raf.readLine();
            Type type = new TypeToken<Record>() {}.getType();
            Record r = JsonPersistence.GSON.fromJson(line, type);
            return r != null ? r.results : null;
        } catch (IOException e) {
            System.err.println("DAO Error: Failed to read history for puzzle " + puzzleId);
            return null;
        }
    }

    private static class Record {
        final int puzzleId;
        final Map<String, PlayerResult> results;
        Record(int id, Map<String, PlayerResult> res) { this.puzzleId = id; this.results = res; }
    }
}
