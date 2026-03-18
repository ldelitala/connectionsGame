package com.connections.server.persistence;

import com.connections.server.models.Puzzle;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages access to puzzle data.
 * Uses an offset-based index to handle potentially large JSON files without loading them fully into RAM.
 */
public class PuzzlesDAO {
    private final String filePath;
    private final Map<Integer, Long> puzzleOffsets = new ConcurrentHashMap<>();
    private final List<Integer> puzzleOrder = new ArrayList<>();
    private int currentIndex = 0;

    private static final Pattern ID_PATTERN = Pattern.compile("\"gameId\"\\s*:\\s*(\\d+)");

    public PuzzlesDAO(String filePath) {
        this.filePath = filePath;
        buildIndex();
    }

    /** Scans the file to map game IDs to their byte positions. */
    private void buildIndex() {
        if (!JsonPersistence.isReadable(filePath)) return;

        try (PushbackInputStream pis = new PushbackInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {
            long offset = 0;
            int b;
            while ((b = pis.read()) != -1) {
                if (b == '{') {
                    pis.unread(b);
                    long start = offset;
                    ExtractedData res = extractJson(pis);
                    Integer id = parseId(res.json);
                    if (id != null) {
                        puzzleOffsets.put(id, start);
                        puzzleOrder.add(id);
                    }
                    offset += res.bytesRead;
                } else {
                    offset++;
                }
            }
            System.out.println("DAO: Indexed " + puzzleOffsets.size() + " puzzles.");
        } catch (IOException e) {
            System.err.println("DAO Error: Failed to index puzzles: " + e.getMessage());
        }
    }

    private Integer parseId(String json) {
        Matcher m = ID_PATTERN.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    public synchronized Puzzle getNextPuzzle() {
        if (puzzleOrder.isEmpty()) return null;
        if (currentIndex >= puzzleOrder.size()) currentIndex = 0;
        return getPuzzle(puzzleOrder.get(currentIndex++));
    }

    public Puzzle getPuzzle(int id) {
        Long offset = puzzleOffsets.get(id);
        if (offset == null) return null;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            fis.getChannel().position(offset);
            return JsonPersistence.GSON.fromJson(extractJson(fis).json, Puzzle.class);
        } catch (IOException e) {
            System.err.println("DAO Error: Failed to read puzzle " + id);
            return null;
        }
    }

    /** Extracts a JSON object by balancing braces. */
    private ExtractedData extractJson(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int balance = 0, b;
        boolean started = false;
        while ((b = is.read()) != -1) {
            baos.write(b);
            if (b == '{') { balance++; started = true; }
            else if (b == '}') balance--;
            if (started && balance == 0) break;
        }
        return new ExtractedData(baos.toString("UTF-8"), baos.size());
    }

    private static class ExtractedData {
        final String json;
        final int bytesRead;
        ExtractedData(String j, int b) { this.json = j; this.bytesRead = b; }
    }

    public synchronized void setNextPuzzleIndex(int idx) { this.currentIndex = (idx >= 0 && idx < puzzleOrder.size()) ? idx : 0; }
    public synchronized int getNextPuzzleIndex() { return currentIndex; }
    public int getNumPuzzles() { return puzzleOrder.size(); }
    public List<Integer> getAllPuzzleIds() { return Collections.unmodifiableList(puzzleOrder); }
}
