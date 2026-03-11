package com.connections.server;

import com.connections.server.models.Puzzle;
import com.connections.common.models.Category;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
        
        // Create a small test games file with pretty printing to ensure each '{' starts a line
        List<Puzzle> puzzles = Arrays.asList(
            new Puzzle(101, Arrays.asList(new Category("G1", Arrays.asList("A", "B", "C", "D")))),
            new Puzzle(202, Arrays.asList(new Category("G2", Arrays.asList("X", "Y", "Z", "W"))))
        );
        
        try (FileWriter writer = new FileWriter(gameFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(puzzles, writer);
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
        Puzzle p1 = gamesDAO.getGame(101);
        assertNotNull(p1);
        assertEquals(101, p1.getGameId());
        assertEquals("G1", p1.getGroups().get(0).getTheme());
        
        Puzzle p2 = gamesDAO.getGame(202);
        assertNotNull(p2);
        assertEquals(202, p2.getGameId());
        assertEquals("G2", p2.getGroups().get(0).getTheme());
    }

    @Test
    public void testSequentialGame() {
        Puzzle p1 = gamesDAO.getNextGame();
        assertEquals(101, p1.getGameId());
        
        Puzzle p2 = gamesDAO.getNextGame();
        assertEquals(202, p2.getGameId());
        
        Puzzle p3 = gamesDAO.getNextGame(); // Wraps around
        assertEquals(101, p3.getGameId());
    }
}
