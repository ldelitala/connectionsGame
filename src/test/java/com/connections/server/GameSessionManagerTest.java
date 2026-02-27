package com.connections.server;

import com.connections.common.Game;
import com.connections.common.User;
import com.connections.common.WordGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameSessionManagerTest {

    private GameManager gameManager;
    private UserManager userManager;
    private UdpNotifier udpNotifier;
    private GameSessionManager sessionManager;

    @BeforeEach
    public void setup() throws IOException {
        gameManager = mock(GameManager.class);
        userManager = mock(UserManager.class);
        udpNotifier = mock(UdpNotifier.class);

        // Mock a single game
        WordGroup group1 = new WordGroup("Animals", Arrays.asList("CAT", "DOG", "FISH", "BIRD"));
        WordGroup group2 = new WordGroup("Colors", Arrays.asList("RED", "GREEN", "BLUE", "YELLOW"));
        WordGroup group3 = new WordGroup("Cities", Arrays.asList("LONDON", "PARIS", "ROME", "BERLIN"));
        WordGroup group4 = new WordGroup("Foods", Arrays.asList("PIZZA", "PASTA", "BURGER", "SUSHI"));
        Game game = new Game(1, Arrays.asList(group1, group2, group3, group4));

        when(gameManager.getNumGames()).thenReturn(1);
        when(gameManager.getRandomGame()).thenReturn(game);
        when(gameManager.getGame(anyInt())).thenReturn(game);

        sessionManager = new GameSessionManager(gameManager, userManager, udpNotifier);
    }

    @Test
    public void testCorrectProposal() {
        User user = new User("test", "pass");
        when(userManager.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "BIRD");
        Map<String, Object> result = sessionManager.submitProposal("test", words);

        assertEquals("ok", result.get("status"));
        assertEquals(1, sessionManager.getPlayerProgress("test").size());
        assertEquals(0, sessionManager.getPlayerMistakes("test"));
    }

    @Test
    public void testWrongProposal() {
        User user = new User("test", "pass");
        when(userManager.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "RED"); // Mixed
        Map<String, Object> result = sessionManager.submitProposal("test", words);

        assertEquals("wrong", result.get("status"));
        assertEquals(0, sessionManager.getPlayerProgress("test").size());
        assertEquals(1, sessionManager.getPlayerMistakes("test"));
    }

    @Test
    public void testWinningGame() {
        User user = mock(User.class);
        when(userManager.getUser("test")).thenReturn(user);

        sessionManager.submitProposal("test", Arrays.asList("CAT", "DOG", "FISH", "BIRD"));
        sessionManager.submitProposal("test", Arrays.asList("RED", "GREEN", "BLUE", "YELLOW"));
        Map<String, Object> result = sessionManager.submitProposal("test", Arrays.asList("LONDON", "PARIS", "ROME", "BERLIN"));

        assertTrue((Boolean) result.get("gameFinished"));
        // Final points: 3 groups (+18) - 0 mistakes (0) = 18
        assertEquals(18, result.get("finalPoints"));
        verify(user).recordWin(0, 18);
    }

    @Test
    public void testLosingGameByMistakes() {
        User user = mock(User.class);
        when(userManager.getUser("test")).thenReturn(user);

        List<String> wrong = Arrays.asList("CAT", "DOG", "FISH", "RED");
        sessionManager.submitProposal("test", wrong);
        sessionManager.submitProposal("test", wrong);
        sessionManager.submitProposal("test", wrong);
        Map<String, Object> result = sessionManager.submitProposal("test", wrong);

        assertTrue((Boolean) result.get("gameFinished"));
        // Final points: 0 groups (0) - 4 mistakes (16) = -16
        assertEquals(-16, result.get("finalPoints"));
        verify(user).recordLoss(4, -16);
    }
}
