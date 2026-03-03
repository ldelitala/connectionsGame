package com.connections.server.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class UserTest {

    @Test
    public void testInitialStats() {
        User user = new User("test", "pass");
        assertEquals(0, user.getPuzzlesCompleted());
        assertEquals(0, user.getTotalScore());
        assertEquals(0.0, user.getWinRate());
        assertEquals(0, user.getCurrentStreak());
    }

    @Test
    public void testWinStats() {
        User user = new User("test", "pass");
        // Correct group 1 (+6), 0 mistakes (0): total 6
        user.recordWin(0, 6); 
        assertEquals(1, user.getPuzzlesCompleted());
        assertEquals(1, user.getWins());
        assertEquals(6, user.getTotalScore());
        assertEquals(1, user.getCurrentStreak());
        assertEquals(1, user.getMaxStreak());
        assertEquals(1, user.getPerfectPuzzles());
        assertEquals(1, user.getMistakeHistogram().get(0));
    }

    @Test
    public void testLossStats() {
        User user = new User("test", "pass");
        // Correct 0 groups (0), 4 mistakes (-16): total -16
        user.recordLoss(4, -16);
        assertEquals(1, user.getPuzzlesCompleted());
        assertEquals(1, user.getLosses());
        assertEquals(-16, user.getTotalScore());
        assertEquals(0, user.getCurrentStreak());
        assertEquals(1, user.getMistakeHistogram().get(4));
    }

    @Test
    public void testStreakAndWinRate() {
        User user = new User("test", "pass");
        user.recordWin(0, 6); // Win
        user.recordWin(1, 2); // Win
        user.recordLoss(4, -16); // Loss
        
        assertEquals(3, user.getPuzzlesCompleted());
        assertEquals(2, user.getWins());
        assertEquals(2.0/3.0 * 100.0, user.getWinRate(), 0.01);
        assertEquals(0, user.getCurrentStreak());
        assertEquals(2, user.getMaxStreak());
    }
}
