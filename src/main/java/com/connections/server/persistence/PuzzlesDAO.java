package com.connections.server.persistence;

import com.connections.server.models.Puzzle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Access Object for Puzzle data.
 * Optimized with an index of file offsets for large JSON files.
 */
public class PuzzlesDAO {
    private final String filePath;
    private final List<Long> puzzleOffsets = new ArrayList<>();
    private int nextPuzzleIndex = 0;
    private final Gson gson = new Gson();

    public PuzzlesDAO(String filePath) {
        this.filePath = filePath;
        buildIndex();
    }

    private void buildIndex() {
        System.out.println("Building puzzle index from " + filePath + "...");
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long pos = 0;
            String line;
            while ((line = raf.readLine()) != null) {
                String trimmed = line.trim();
                // A puzzle object in the provided JSON format starts with '{'
                // We look for the start of an object that is likely a Puzzle (contains gameId on next lines)
                // To be safe and fast, we just index every line starting with '{' 
                // and verify it's a puzzle when reading.
                if (trimmed.equals("{")) {
                    puzzleOffsets.add(pos);
                }
                pos = raf.getFilePointer();
            }
            System.out.println("Indexed " + puzzleOffsets.size() + " potential puzzles.");
        } catch (IOException e) {
            System.err.println("Critical error building puzzle index: " + e.getMessage());
        }
    }

    public synchronized Puzzle getNextPuzzle() {
        if (puzzleOffsets.isEmpty()) return null;
        
        Puzzle puzzle = null;
        int attempts = 0;
        while (puzzle == null && attempts < puzzleOffsets.size()) {
            if (nextPuzzleIndex >= puzzleOffsets.size()) {
                System.out.println("End of puzzle list. Restarting from the first puzzle.");
                nextPuzzleIndex = 0;
            }
            puzzle = getPuzzleByOffset(puzzleOffsets.get(nextPuzzleIndex));
            nextPuzzleIndex++;
            attempts++;
        }
        return puzzle;
    }

    public Puzzle getPuzzleById(int id) {
        for (Long offset : puzzleOffsets) {
            Puzzle p = getPuzzleByOffset(offset);
            if (p != null && p.getPuzzleId() == id) return p;
        }
        return null;
    }

    private Puzzle getPuzzleByOffset(long offset) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(offset);
            
            StringBuilder sb = new StringBuilder();
            int balance = 0;
            boolean started = false;
            int c;
            while ((c = raf.read()) != -1) {
                char charC = (char) c;
                if (charC == '{') {
                    balance++;
                    started = true;
                } else if (charC == '}') {
                    balance--;
                }
                
                if (started) sb.append(charC);
                if (started && balance == 0) break;
            }
            
            String json = sb.toString();
            if (json.contains("\"gameId\"")) {
                return gson.fromJson(json, Puzzle.class);
            }
            return null;
        } catch (IOException e) {
            System.err.println("Error reading puzzle at offset " + offset + ": " + e.getMessage());
            return null;
        }
    }

    public Puzzle getPuzzle(int puzzleId) {
        return getPuzzleById(puzzleId);
    }

    public int getNumPuzzles() {
        // Return count of valid puzzles
        return (int) puzzleOffsets.stream()
                .map(this::getPuzzleByOffset)
                .filter(java.util.Objects::nonNull)
                .count();
    }

    public List<Integer> getAllPuzzleIds() {
        return puzzleOffsets.stream()
                .map(this::getPuzzleByOffset)
                .filter(java.util.Objects::nonNull)
                .map(Puzzle::getPuzzleId)
                .collect(Collectors.toList());
    }
}
