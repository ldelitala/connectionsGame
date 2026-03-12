package com.connections.server.persistence;

import com.connections.server.models.Puzzle;
import com.connections.common.models.Category;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PuzzlesDAOPerformanceTest {

    @TempDir
    Path tempDir;

    @Test
    public void testRealDataPerformance() {
        String realFile = "data/Connections_Data.json";
        File file = new File(realFile);
        if (!file.exists()) {
            System.out.println("Real data file not found, skipping real performance test.");
            return;
        }

        long start = System.nanoTime();
        PuzzlesDAO dao = new PuzzlesDAO(realFile);
        long end = System.nanoTime();

        double durationMs = (end - start) / 1_000_000.0;
        long fileSize = file.length();
        int count = dao.getNumPuzzles();

        System.out.println("\n--- Real Data Performance (912 Puzzles) ---");
        System.out.println("Indexing Time: " + String.format("%.2f", durationMs) + " ms");
        System.out.println("File Size: " + (fileSize / 1024) + " KB");
        System.out.println("Throughput: " + String.format("%.2f", (fileSize / 1024.0 / 1024.0) / (durationMs / 1000.0)) + " MB/s");
    }

    @Test
    public void testExtrapolatedPerformance() throws IOException {
        // Create a synthetic file of ~10MB
        Path largeFile = tempDir.resolve("large_puzzles.json");
        int numPuzzles = 20000; 
        
        System.out.println("\nGenerating synthetic 10MB test file...");
        List<Puzzle> puzzles = new ArrayList<>();
        Category cat = new Category("Test", Arrays.asList("W1", "W2", "W3", "W4"));
        for (int i = 0; i < numPuzzles; i++) {
            puzzles.add(new Puzzle(i, Arrays.asList(cat)));
        }

        try (FileWriter writer = new FileWriter(largeFile.toFile())) {
            new Gson().toJson(puzzles, writer);
        }

        long fileSize = largeFile.toFile().length();
        
        // Measure 5 runs to get an average
        int runs = 5;
        double totalTimeMs = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            new PuzzlesDAO(largeFile.toString());
            long end = System.nanoTime();
            totalTimeMs += (end - start) / 1_000_000.0;
        }
        
        double avgTimeMs = totalTimeMs / runs;
        double throughputMBs = (fileSize / 1024.0 / 1024.0) / (avgTimeMs / 1000.0);

        System.out.println("--- Synthetic Data Performance (" + numPuzzles + " Puzzles) ---");
        System.out.println("Average Indexing Time: " + String.format("%.2f", avgTimeMs) + " ms");
        System.out.println("File Size: " + String.format("%.2f", fileSize / 1024.0 / 1024.0) + " MB");
        System.out.println("Throughput: " + String.format("%.2f", throughputMBs) + " MB/s");
        
        System.out.println("\n--- Estimation for 1 GB File ---");
        double estSeconds = 1024.0 / throughputMBs;
        System.out.println("Estimated Indexing Time: " + String.format("%.2f", estSeconds) + " seconds");
    }
}
