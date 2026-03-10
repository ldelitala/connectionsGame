package com.connections.server;

import com.connections.server.models.Game;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Game data.
 * Optimized with an index of file offsets for large JSON files.
 */
public class GamesDAO {
    private final String filePath;
    private final List<Long> gameOffsets = new ArrayList<>();
    private int nextGameIndex = 0;
    private final Gson gson = new Gson();

    public GamesDAO(String filePath) {
        this.filePath = filePath;
        buildIndex();
    }

    private void buildIndex() {
        System.out.println("Building game index from " + filePath + "...");
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long pos = 0;
            String line;
            while ((line = raf.readLine()) != null) {
                if (line.trim().startsWith("{") && line.contains("\"gameId\"")) {
                    gameOffsets.add(pos);
                }
                pos = raf.getFilePointer();
            }
            System.out.println("Indexed " + gameOffsets.size() + " games.");
        } catch (IOException e) {
            System.err.println("Critical error building game index: " + e.getMessage());
        }
    }

    public synchronized Game getNextGame() {
        if (gameOffsets.isEmpty()) return null;
        if (nextGameIndex >= gameOffsets.size()) {
            System.out.println("All games have been played. Restarting from the first game.");
            nextGameIndex = 0;
        }
        Game game = getGameByOffset(gameOffsets.get(nextGameIndex));
        nextGameIndex++;
        return game;
    }

    public Game getGameById(int id) {
        // Simplified: in a real scenario we'd use a Map<Id, Offset>
        for (Long offset : gameOffsets) {
            Game g = getGameByOffset(offset);
            if (g != null && g.getGameId() == id) return g;
        }
        return null;
    }

    private Game getGameByOffset(long offset) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(offset);
            
            // Read until the end of the JSON object
            StringBuilder sb = new StringBuilder();
            int balance = 0;
            boolean started = false;
            int c;
            while ((c = raf.read()) != -1) {
                char charC = (char) c;
                if (charC == '{') {
                    balance++;
                    started = true;
                }
                if (charC == '}') balance--;
                
                if (started) sb.append(charC);
                if (started && balance == 0) break;
            }
            return gson.fromJson(sb.toString(), Game.class);
        } catch (IOException e) {
            System.err.println("Error reading game at offset " + offset + ": " + e.getMessage());
            return null;
        }
    }

    public Game getGame(int gameId) {
        return getGameById(gameId);
    }
}
