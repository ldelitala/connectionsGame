package com.connections.server;

import com.connections.server.models.Puzzle;
import com.connections.server.models.User;
import com.connections.common.models.Category;
import com.connections.common.dto.PuzzleStateDTO;
import com.connections.common.dto.GuessResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameSessionManagerTest {

    private GamesDAO gamesDAO;
    private UserManager userManager;
    private UdpNotifier udpNotifier;
    private GameSessionManager sessionManager;

    @BeforeEach
    public void setup() throws IOException {
        gamesDAO = mock(GamesDAO.class);
        userManager = mock(UserManager.class);
        udpNotifier = mock(UdpNotifier.class);

        // Mock a single puzzle
        Category group1 = new Category("Animals", Arrays.asList("CAT", "DOG", "FISH", "BIRD"));
        Category group2 = new Category("Colors", Arrays.asList("RED", "GREEN", "BLUE", "YELLOW"));
        Category group3 = new Category("Cities", Arrays.asList("LONDON", "PARIS", "ROME", "BERLIN"));
        Category group4 = new Category("Foods", Arrays.asList("PIZZA", "PASTA", "BURGER", "SUSHI"));
        Puzzle puzzle = new Puzzle(1, Arrays.asList(group1, group2, group3, group4));

        when(gamesDAO.getNumGames()).thenReturn(1);
        when(gamesDAO.getNextGame()).thenReturn(puzzle);
        when(gamesDAO.getGame(anyInt())).thenReturn(puzzle);

        sessionManager = new GameSessionManager(gamesDAO, userManager, udpNotifier, 300000);
    }

    @Test
    public void testCorrectProposal() {
        User user = new User("test", "pass");
        when(userManager.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "BIRD");
        GuessResultDTO result = sessionManager.submitProposal("test", words);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals(6.0f, result.getPointsDelta());
        assertEquals(1, sessionManager.getPlayerProgress("test").size());
        assertEquals(0, sessionManager.getPlayerMistakes("test"));
    }

    @Test
    public void testWrongProposal() {
        User user = new User("test", "pass");
        when(userManager.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "RED"); // Mixed
        GuessResultDTO result = sessionManager.submitProposal("test", words);

        assertNotNull(result);
        assertEquals("wrong", result.getStatus());
        assertEquals(-4.0f, result.getPointsDelta());
        assertEquals(0, sessionManager.getPlayerProgress("test").size());
        assertEquals(1, sessionManager.getPlayerMistakes("test"));
    }

    @Test
    public void testWinningGame() {
        User user = mock(User.class);
        when(userManager.getUser("test")).thenReturn(user);

        sessionManager.submitProposal("test", Arrays.asList("CAT", "DOG", "FISH", "BIRD"));
        sessionManager.submitProposal("test", Arrays.asList("RED", "GREEN", "BLUE", "YELLOW"));
        
        GuessResultDTO result = sessionManager.submitProposal("test", Arrays.asList("LONDON", "PARIS", "ROME", "BERLIN"));

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        // Final points: 3 groups indovinati (+18) - 0 errori (0) = 18
        assertEquals(18, result.getTotalPoints());
        verify(user).recordWin(0, 18);
    }

    @Test
    public void testGetPersonalGameStatusWithRemainingWords() {
        User user = new User("test", "pass");
        when(userManager.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "BIRD");
        sessionManager.submitProposal("test", words);

        PuzzleStateDTO status = sessionManager.getPersonalGameStatus("test", 1);
        
        assertNotNull(status);
        assertFalse(status.isFinished());
        List<String> remainingWords = status.getRemainingWords();
        assertEquals(12, remainingWords.size());
        assertFalse(remainingWords.contains("CAT"));
        assertTrue(remainingWords.contains("RED"));
    }
}
