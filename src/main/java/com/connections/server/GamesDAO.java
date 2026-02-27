package com.connections.server;

import com.connections.common.Game;
import com.google.gson.Gson;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the collection of available games using file indexing. Loads only byte offsets into
 * memory, reading games on demand.
 */
public class GamesDAO {
    private final String filePath;
    private final Map<Integer, Long> index = new ConcurrentHashMap<>();
    private final List<Integer> gameIds = new ArrayList<>();
    private final Gson gson = new Gson();

    private static final Pattern GAME_ID_PATTERN = Pattern.compile("\"gameId\"\\s*:\\s*(\\d+)");

    public GamesDAO(String filePath) throws IOException {
        this.filePath = filePath;
        buildIndex();
    }

    private void buildIndex() throws IOException {
        System.out.println("Building game index from " + filePath + "...");

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath))) {
            long currentOffset = 0;
            int b;

            while ((b = bis.read()) != -1) {
                if (b == '{') {
                    long startOffset = currentOffset;

                    String restOfJson = extractRestOfObject(bis);
                    String fullJson = "{" + restOfJson;

                    currentOffset += 1 + restOfJson.length();

                    Integer gameId = extractGameId(fullJson);
                    if (gameId != null) {
                        index.put(gameId, startOffset);
                        gameIds.add(gameId);
                    }
                } else {
                    currentOffset++;
                }
            }
        } catch (IOException e) {
            System.err.println("Error building game index: " + e.getMessage());
            throw e;
        }

        System.out.println("Indexed " + index.size() + " games.");
    }

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

    private Integer extractGameId(String json) {
        Matcher matcher = GAME_ID_PATTERN.matcher(json);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    public Game getGame(int gameId) {
        Long offset = index.get(gameId);
        if (offset == null)
            return null;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            fis.getChannel().position(offset);

            if (fis.read() == '{') {
                String restOfJson = extractRestOfObject(fis);
                return gson.fromJson("{" + restOfJson, Game.class);
            }
        } catch (IOException e) {
            System.err.println("Error reading game " + gameId + " from disk: " + e.getMessage());
        }
        return null;
    }

    public int getNumGames() {
        return gameIds.size();
    }

    public List<Integer> getAllGameIds() {
        return Collections.unmodifiableList(gameIds);
    }

    public Game getRandomGame() {
        if (gameIds.isEmpty())
            return null;
        int randomId = gameIds.get(new java.util.Random().nextInt(gameIds.size()));
        return getGame(randomId);
    }
}
