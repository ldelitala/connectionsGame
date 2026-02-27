package com.connections.server;

import com.connections.common.Game;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the collection of available games using file indexing.
 * Loads only byte offsets into memory, reading games on demand.
 */
public class GameManager {
    private final String filePath;
    private final Map<Integer, Long> index = new ConcurrentHashMap<>();
    private final List<Integer> gameIds = new ArrayList<>();
    private final Gson gson = new Gson();

    public GameManager(String filePath) throws IOException {
        this.filePath = filePath;
        buildIndex();
    }

    /**
     * Scans the file to find the byte offset of each game object.
     * Uses JsonReader to stream through the file without loading it all into memory.
     */
    private void buildIndex() throws IOException {
        System.out.println("Building game index from " + filePath + "...");
        
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {            
            long PointerPos = 0;
            long length = raf.length();
            
            while (PointerPos < length) {
                raf.seek(PointerPos);
                int b = raf.read();
                if (b == '{') {
                    long startOffset = PointerPos;
                    byte[] chunk = new byte[256];
                    raf.read(chunk);
                    String s = new String(chunk);
                    int idIndex = s.indexOf("\"gameId\":");
                    if (idIndex != -1) {
                        int start = idIndex + 9;
                        while (start < s.length() && !Character.isDigit(s.charAt(start))) start++;
                        int end = start;
                        while (end < s.length() && Character.isDigit(s.charAt(end))) end++;
                        if (start < end) {
                            int id = Integer.parseInt(s.substring(start, end));
                            index.put(id, startOffset);
                            gameIds.add(id);
                        }
                    }
                    // Skip this object by balancing braces
                    int balance = 1;
                    PointerPos++;
                    while (balance > 0 && PointerPos < length) {
                        raf.seek(PointerPos);
                        int next = raf.read();
                        if (next == '{') balance++;
                        else if (next == '}') balance--;
                        PointerPos++;
                    }
                    continue; // PointerPos already advanced
                }
                PointerPos++;
            }
        }
        System.out.println("Indexed " + index.size() + " games.");
    }

    /**
     * Reads a specific game from disk using the stored offset.
     */
    public Game getGame(int gameId) {
        Long offset = index.get(gameId);
        if (offset == null) return null;

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(offset);
            
            // Read until the object is balanced
            StringBuilder sb = new StringBuilder();
            int balance = 0;
            boolean started = false;
            while (true) {
                int b = raf.read();
                if (b == -1) break;
                char c = (char) b;
                if (c == '{') {
                    balance++;
                    started = true;
                } else if (c == '}') {
                    balance--;
                }
                if (started) sb.append(c);
                if (started && balance == 0) break;
            }
            return gson.fromJson(sb.toString(), Game.class);
        } catch (IOException e) {
            System.err.println("Error reading game " + gameId + " from disk: " + e.getMessage());
            return null;
        }
    }

    public int getNumGames() {
        return gameIds.size();
    }

    public List<Integer> getAllGameIds() {
        return Collections.unmodifiableList(gameIds);
    }
    
    /**
     * Returns a random game from the indexed IDs.
     */
    public Game getRandomGame() {
        if (gameIds.isEmpty()) return null;
        int randomId = gameIds.get(new java.util.Random().nextInt(gameIds.size()));
        return getGame(randomId);
    }
}
