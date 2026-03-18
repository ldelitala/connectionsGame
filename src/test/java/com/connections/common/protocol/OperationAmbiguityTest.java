package com.connections.common.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

public class OperationAmbiguityTest {

    private final Gson gson = new Gson();

    @Test
    public void testResolvePuzzleInfoAmbiguity() {
        // Case 1: No gameId -> Should be GET_PUZZLE_INFO (Current)
        String jsonNoId = "{\"operation\": \"requestGameInfo\"}";
        Operation opNoId = Operation.resolve(jsonNoId, gson);
        assertEquals(Operation.GET_PUZZLE_INFO, opNoId, 
                     "Should resolve to CURRENT puzzle info when gameId is missing");

        // Case 2: With gameId -> Should be GET_PAST_PUZZLE_INFO (Past)
        String jsonWithId = "{\"operation\": \"requestGameInfo\", \"gameId\": 123}";
        Operation opWithId = Operation.resolve(jsonWithId, gson);
        assertEquals(Operation.GET_PAST_PUZZLE_INFO, opWithId, 
                     "Should resolve to PAST puzzle info when gameId is present");
    }
    
    @Test
    public void testResolvePuzzleStatsAmbiguity() {
        // Case 1: No gameId -> Should be GET_PUZZLE_STATS (Current)
        String jsonNoId = "{\"operation\": \"requestGameStats\"}";
        Operation opNoId = Operation.resolve(jsonNoId, gson);
        assertEquals(Operation.GET_PUZZLE_STATS, opNoId, 
                     "Should resolve to CURRENT puzzle stats when gameId is missing");

        // Case 2: With gameId -> Should be GET_PAST_PUZZLE_STATS (Past)
        String jsonWithId = "{\"operation\": \"requestGameStats\", \"gameId\": 123}";
        Operation opWithId = Operation.resolve(jsonWithId, gson);
        assertEquals(Operation.GET_PAST_PUZZLE_STATS, opWithId, 
                     "Should resolve to PAST puzzle stats when gameId is present");
    }
}
