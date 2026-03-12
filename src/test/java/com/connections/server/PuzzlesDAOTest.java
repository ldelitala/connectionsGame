package com.connections.server;

import com.connections.server.models.Puzzle;
import com.connections.server.persistence.PuzzlesDAO;
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

public class PuzzlesDAOTest {

    @TempDir
    Path tempDir;

    private String puzzleFile;
    private PuzzlesDAO puzzlesDAO;

    @BeforeEach
    public void setup() throws IOException {
        puzzleFile = tempDir.resolve("puzzles.json").toString();
        
        // Create a small test puzzles file with pretty printing to ensure each '{' starts a line
        List<Puzzle> puzzles = Arrays.asList(
            new Puzzle(101, Arrays.asList(new Category("G1", Arrays.asList("A", "B", "C", "D")))),
            new Puzzle(202, Arrays.asList(new Category("G2", Arrays.asList("X", "Y", "Z", "W"))))
        );
        
        try (FileWriter writer = new FileWriter(puzzleFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(puzzles, writer);
        }
        
        puzzlesDAO = new PuzzlesDAO(puzzleFile);
    }

    @Test
    public void testIndexing() {
        assertEquals(2, puzzlesDAO.getNumPuzzles());
        assertTrue(puzzlesDAO.getAllPuzzleIds().contains(101));
        assertTrue(puzzlesDAO.getAllPuzzleIds().contains(202));
    }

    @Test
    public void testOnDemandReading() {
        Puzzle p1 = puzzlesDAO.getPuzzle(101);
        assertNotNull(p1);
        assertEquals(101, p1.getPuzzleId());
        assertEquals("G1", p1.getGroups().get(0).getTheme());
        
        Puzzle p2 = puzzlesDAO.getPuzzle(202);
        assertNotNull(p2);
        assertEquals(202, p2.getPuzzleId());
        assertEquals("G2", p2.getGroups().get(0).getTheme());
    }

    @Test
    public void testSequentialPuzzle() {
        Puzzle p1 = puzzlesDAO.getNextPuzzle();
        assertEquals(101, p1.getPuzzleId());
        
        Puzzle p2 = puzzlesDAO.getNextPuzzle();
        assertEquals(202, p2.getPuzzleId());
        
        Puzzle p3 = puzzlesDAO.getNextPuzzle(); // Wraps around
        assertEquals(101, p3.getPuzzleId());
    }
}
