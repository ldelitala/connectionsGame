package com.connections.server.persistence;

import com.connections.server.models.PlayerResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object for historical puzzle results.
 * Optimized for low RAM usage by using a file offset index.
 */
public class HistoryDAO {
    private final String filePath;
    private final Gson gson = new Gson();
    
    // Map of puzzleId -> File Offset for fast lookups
    private final Map<Integer, Long> puzzleOffsets = new ConcurrentHashMap<>();

    public HistoryDAO(String filePath) {
        this.filePath = filePath;
        buildIndex();
    }

    private void buildIndex() {
        File file = new File(filePath);
        if (!file.exists()) return;

        System.out.println("Indexing puzzle history from " + filePath + "...");
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long pos = 0;
            String line;
            while ((line = raf.readLine()) != null) {
                // Each line is a JSON object containing puzzleId
                // Format expected: {"puzzleId": 1, "results": {...}}
                if (line.contains("\"puzzleId\":")) {
                    int puzzleId = extractPuzzleId(line);
                    if (puzzleId != -1) {
                        puzzleOffsets.put(puzzleId, pos);
                    }
                }
                pos = raf.getFilePointer();
            }
            System.out.println("Indexed " + puzzleOffsets.size() + " historical puzzles.");
        } catch (IOException e) {
            System.err.println("Error indexing history: " + e.getMessage());
        }
    }

    private int extractPuzzleId(String json) {
        // Simple regex-less extraction for performance
        try {
            int start = json.indexOf("\"puzzleId\":") + 11;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Appends a puzzle's results to the log file.
     */
    public synchronized void archivePuzzle(int puzzleId, Map<String, PlayerResult> results) {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            long offset = raf.length();
            raf.seek(offset);
            
            // Wrap in a simple container for the log
            PuzzleRecord record = new PuzzleRecord(puzzleId, results);
            String json = gson.toJson(record) + "\n";
            
            raf.write(json.getBytes("UTF-8"));
            puzzleOffsets.put(puzzleId, offset);
            
        } catch (IOException e) {
            System.err.println("Error archiving puzzle " + puzzleId + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves results for a specific past puzzle.
     */
    public Map<String, PlayerResult> getPuzzleResults(int puzzleId) {
        Long offset = puzzleOffsets.get(puzzleId);
        if (offset == null) return null;

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(offset);
            String line = raf.readLine();
            if (line == null) return null;
            
            Type type = new TypeToken<PuzzleRecord>() {}.getType();
            PuzzleRecord record = gson.fromJson(line, type);
            return record.results;
            
        } catch (IOException e) {
            System.err.println("Error reading historical puzzle " + puzzleId + ": " + e.getMessage());
            return null;
        }
    }

    private static class PuzzleRecord {
        final int puzzleId;
        final Map<String, PlayerResult> results;

        PuzzleRecord(int puzzleId, Map<String, PlayerResult> results) {
            this.puzzleId = puzzleId;
            this.results = results;
        }
    }
}
