package com.connections.common.protocol;

import com.connections.common.protocol.client_requests.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class RequestValidationTest {

    @Test
    public void testUserRequestValidation() {
        // Valid register
        assertTrue(UserRequest.register("user", "pass").isValid());
        // Invalid register (empty)
        assertFalse(UserRequest.register("", "pass").isValid());
        assertFalse(UserRequest.register("user", " ").isValid());

        // Valid login
        assertTrue(UserRequest.login("user", "pass", 8080).isValid());
        
        // Update credentials
        // Both new name and new pass
        assertTrue(UserRequest.updateCredentials("oldU", "oldP", "newU", "newP").isValid());
        // Only new name
        assertTrue(UserRequest.updateCredentials("oldU", "oldP", "newU", null).isValid());
        // Only new pass
        assertTrue(UserRequest.updateCredentials("oldU", "oldP", null, "newP").isValid());
        // Neither (invalid)
        assertFalse(UserRequest.updateCredentials("oldU", "oldP", null, null).isValid());
        // Missing old credentials (invalid)
        assertFalse(UserRequest.updateCredentials(null, "oldP", "newU", "newP").isValid());
    }

    @Test
    public void testHistoryRequestValidation() {
        // Valid past info/stats
        assertTrue(HistoryRequest.getPuzzleInfo(1).isValid());
        assertTrue(HistoryRequest.getPuzzleStats(1).isValid());

        // Leaderboard
        // Full leaderboard (both null)
        assertTrue(HistoryRequest.getLeaderboard().isValid());
        // Top N (playerName null)
        assertTrue(HistoryRequest.getLeaderboard(10).isValid());
        // Specific player (topK null)
        assertTrue(HistoryRequest.getLeaderboard("player").isValid());
        
        // Both present (invalid according to XOR logic in my replace, or updated logic)
        // Wait, my updated logic was return !(playerName != null && topPlayers != null);
        // So both present is invalid.
        // I can't easily create a "both present" one via factory methods, but I can check the constructor if it were public.
    }

    @Test
    public void testGameplayRequestValidation() {
        // Valid proposal (4 words)
        assertTrue(GameplayRequest.submitProposal(Arrays.asList("A", "B", "C", "D")).isValid());
        // Invalid proposal (not 4 words)
        assertFalse(GameplayRequest.submitProposal(Arrays.asList("A", "B", "C")).isValid());
        assertFalse(GameplayRequest.submitProposal(Arrays.asList("A", "B", "C", "D", "E")).isValid());
        
        // Info/Stats
        assertTrue(GameplayRequest.getCurrentInfo().isValid());
        assertTrue(GameplayRequest.getCurrentStats().isValid());
    }
}
