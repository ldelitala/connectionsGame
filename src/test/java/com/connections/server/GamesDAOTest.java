package com.connections.server;

import com.connections.common.Game;
import com.connections.common.WordGroup;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GamesDAOTest {

    @TempDir
    Path tempDir;

    private String gameFile;
    private GamesDAO gamesDAO;

    @BeforeEach
    public void setup() throws IOException {
        gameFile = tempDir.resolve("games.json").toString();
        
        // Create a small test games file
        List<Game> games = Arrays.asList(
            new Game(101, Arrays.asList(new WordGroup("G1", Arrays.asList("A", "B", "C", "D")))),
            new Game(202, Arrays.asList(new WordGroup("G2", Arrays.asList("X", "Y", "Z", "W"))))
        );
        
        try (FileWriter writer = new FileWriter(gameFile)) {
            new Gson().toJson(games, writer);
        }
        
        gamesDAO = new GamesDAO(gameFile);
    }

    @Test
    public void testIndexing() {
        assertEquals(2, gamesDAO.getNumGames());
        assertTrue(gamesDAO.getAllGameIds().contains(101));
        assertTrue(gamesDAO.getAllGameIds().contains(202));
    }

    @Test
    public void testOnDemandReading() {
        Game g1 = gamesDAO.getGame(101);
        assertNotNull(g1);
        assertEquals(101, g1.getGameId());
        assertEquals("G1", g1.getGroups().get(0).getTheme());
        
        Game g2 = gamesDAO.getGame(202);
        assertNotNull(g2);
        assertEquals(202, g2.getGameId());
        assertEquals("G2", g2.getGroups().get(0).getTheme());
    }

    @Test
    public void testSequentialGame() {
        Game g1 = gamesDAO.getNextGame();
        assertEquals(101, g1.getGameId());
        
        Game g2 = gamesDAO.getNextGame();
        assertEquals(202, g2.getGameId());
        
        Game g3 = gamesDAO.getNextGame(); // Wraps around
        assertEquals(101, g3.getGameId());
    }
}
