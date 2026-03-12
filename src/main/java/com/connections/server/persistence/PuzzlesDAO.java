package com.connections.server.persistence;

import com.connections.server.models.Puzzle;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance DAO for Puzzles.
 * Uses byte-offset indexing and on-demand reading to minimize RAM usage.
 * Implementation follows the optimized GamesDAO pattern.
 */
public class PuzzlesDAO {
    private final String filePath;
    private final Map<Integer, Long> index = new ConcurrentHashMap<>();
    private final List<Integer> puzzleIds = new ArrayList<>();
    private int nextPuzzleIndex = 0;
    private final Gson gson = new Gson();

    private static final Pattern GAME_ID_PATTERN = Pattern.compile("\"gameId\"\\s*:\\s*(\\d+)");

    public PuzzlesDAO(String filePath) {
        this.filePath = filePath;
        try {
            buildIndex();
        } catch (IOException e) {
            System.err.println("Critical error building puzzle index: " + e.getMessage());
        }
    }

    /**
     * Scans the JSON file and indexes the byte offset of each puzzle object.
     */
    private void buildIndex() throws IOException {
        System.out.println("Building puzzle index from " + filePath + "...");

        // PushbackInputStream allows us to "peek" at bytes using unread()
        try (PushbackInputStream pis = new PushbackInputStream(new BufferedInputStream(new FileInputStream(filePath)), 1)) {
            long currentOffset = 0;
            int b;

            while ((b = pis.read()) != -1) {
                if (b == '{') {
                    // --- PEEK EXAMPLE ---
                    // We can see the next byte without moving the pointer
                    int nextByte = pis.read();
                    if (nextByte != -1) {
                        pis.unread(nextByte); // Put it back immediately
                    }
                    // --------------------

                    long startOffset = currentOffset;

                    // Extract the rest of the object to calculate its length and get the ID
                    String restOfJson = extractRestOfObject(pis);
                    String fullJson = "{" + restOfJson;

                    currentOffset += 1 + restOfJson.length();

                    Integer puzzleId = extractPuzzleId(fullJson);
                    if (puzzleId != null) {
                        index.put(puzzleId, startOffset);
                        puzzleIds.add(puzzleId);
                    }
                } else {
                    currentOffset++;
                }
            }
        }
        System.out.println("Indexed " + index.size() + " puzzles.");
    }

    /**
     * Reads until the braces are balanced to extract a full JSON object.
     */
    private String extractRestOfObject(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        int balance = 1;
        int b;

        while (balance > 0 && (b = is.read()) != -1) {
            char c = (char) b;
            sb.append(c);

            if (c == '{') {
                balance++;
            } else if (c == '}') {
                balance--;
            }
        }
        return sb.toString();
    }

    private Integer extractPuzzleId(String json) {
        Matcher matcher = GAME_ID_PATTERN.matcher(json);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    /**
     * Returns the next puzzle in the sequence, wrapping around if necessary.
     */
    public synchronized Puzzle getNextPuzzle() {
        if (puzzleIds.isEmpty()) return null;
        
        if (nextPuzzleIndex >= puzzleIds.size()) {
            nextPuzzleIndex = 0;
        }
        
        Puzzle p = getPuzzle(puzzleIds.get(nextPuzzleIndex));
        nextPuzzleIndex++;
        return p;
    }

    /**
     * Reads a specific puzzle from disk using its byte offset.
     */
    public Puzzle getPuzzle(int puzzleId) {
        Long offset = index.get(puzzleId);
        if (offset == null) return null;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            // Jump directly to the offset
            fis.getChannel().position(offset);

            if (fis.read() == '{') {
                String restOfJson = extractRestOfObject(fis);
                return gson.fromJson("{" + restOfJson, Puzzle.class);
            }
        } catch (IOException e) {
            System.err.println("Error reading puzzle " + puzzleId + " from disk: " + e.getMessage());
        }
        return null;
    }

    public int getNumPuzzles() {
        return puzzleIds.size();
    }

    public List<Integer> getAllPuzzleIds() {
        return Collections.unmodifiableList(puzzleIds);
    }
}
