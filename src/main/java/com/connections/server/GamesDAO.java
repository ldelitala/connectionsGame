package com.connections.server;

import com.connections.server.models.Game;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                String trimmed = line.trim();
                // A game object in the provided JSON format starts with '{'
                // We look for the start of an object that is likely a Game (contains gameId on next lines)
                // To be safe and fast, we just index every line starting with '{' 
                // and verify it's a game when reading.
                if (trimmed.equals("{")) {
                    gameOffsets.add(pos);
                }
                pos = raf.getFilePointer();
            }
            System.out.println("Indexed " + gameOffsets.size() + " potential games.");
        } catch (IOException e) {
            System.err.println("Critical error building game index: " + e.getMessage());
        }
    }

    public synchronized Game getNextGame() {
        if (gameOffsets.isEmpty()) return null;
        
        Game game = null;
        int attempts = 0;
        while (game == null && attempts < gameOffsets.size()) {
            if (nextGameIndex >= gameOffsets.size()) {
                System.out.println("End of game list. Restarting from the first game.");
                nextGameIndex = 0;
            }
            game = getGameByOffset(gameOffsets.get(nextGameIndex));
            nextGameIndex++;
            attempts++;
        }
        return game;
    }

    public Game getGameById(int id) {
        for (Long offset : gameOffsets) {
            Game g = getGameByOffset(offset);
            if (g != null && g.getGameId() == id) return g;
        }
        return null;
    }

    private Game getGameByOffset(long offset) {
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
                return gson.fromJson(json, Game.class);
            }
            return null;
        } catch (IOException e) {
            System.err.println("Error reading game at offset " + offset + ": " + e.getMessage());
            return null;
        }
    }

    public Game getGame(int gameId) {
        return getGameById(gameId);
    }

    public int getNumGames() {
        // Return count of valid games
        return (int) gameOffsets.stream()
                .map(this::getGameByOffset)
                .filter(java.util.Objects::nonNull)
                .count();
    }

    public List<Integer> getAllGameIds() {
        return gameOffsets.stream()
                .map(this::getGameByOffset)
                .filter(java.util.Objects::nonNull)
                .map(Game::getGameId)
                .collect(Collectors.toList());
    }
}
