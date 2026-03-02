package com.connections.server;

import com.connections.common.Game;
import com.connections.common.User;
import com.connections.common.WordGroup;
import com.connections.common.dto.CurrentGameStatusDTO;
import com.connections.common.dto.GameStatusDTO;
import com.connections.common.dto.ProposalResultDTO;
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

        // Mock a single game
        WordGroup group1 = new WordGroup("Animals", Arrays.asList("CAT", "DOG", "FISH", "BIRD"));
        WordGroup group2 = new WordGroup("Colors", Arrays.asList("RED", "GREEN", "BLUE", "YELLOW"));
        WordGroup group3 = new WordGroup("Cities", Arrays.asList("LONDON", "PARIS", "ROME", "BERLIN"));
        WordGroup group4 = new WordGroup("Foods", Arrays.asList("PIZZA", "PASTA", "BURGER", "SUSHI"));
        Game game = new Game(1, Arrays.asList(group1, group2, group3, group4));

        when(gamesDAO.getNumGames()).thenReturn(1);
        when(gamesDAO.getNextGame()).thenReturn(game);
        when(gamesDAO.getGame(anyInt())).thenReturn(game);

        sessionManager = new GameSessionManager(gamesDAO, userManager, udpNotifier, 300000);
    }

    @Test
    public void testCorrectProposal() {
        User user = new User("test", "pass");
        when(userManager.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "BIRD");
        ProposalResultDTO result = sessionManager.submitProposal("test", words);

        assertEquals("ok", result.getStatus());
        assertEquals(1, sessionManager.getPlayerProgress("test").size());
        assertEquals(0, sessionManager.getPlayerMistakes("test"));
    }

    @Test
    public void testWrongProposal() {
        User user = new User("test", "pass");
        when(userManager.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "RED"); // Mixed
        ProposalResultDTO result = sessionManager.submitProposal("test", words);

        assertEquals("wrong", result.getStatus());
        assertEquals(0, sessionManager.getPlayerProgress("test").size());
        assertEquals(1, sessionManager.getPlayerMistakes("test"));
    }

    @Test
    public void testWinningGame() {
        User user = mock(User.class);
        when(userManager.getUser("test")).thenReturn(user);

        sessionManager.submitProposal("test", Arrays.asList("CAT", "DOG", "FISH", "BIRD"));
        sessionManager.submitProposal("test", Arrays.asList("RED", "GREEN", "BLUE", "YELLOW"));
        ProposalResultDTO result = sessionManager.submitProposal("test", Arrays.asList("LONDON", "PARIS", "ROME", "BERLIN"));

        assertTrue(result.getGameFinished());
        // Final points: 3 groups (+18) - 0 mistakes (0) = 18
        assertEquals(18, result.getFinalPoints());
        verify(user).recordWin(0, 18);
    }

    @Test
    public void testGetPersonalGameStatusWithRemainingWords() {
        User user = new User("test", "pass");
        when(userManager.getUser("test")).thenReturn(user);

        // One correct group
        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "BIRD");
        sessionManager.submitProposal("test", words);

        GameStatusDTO status = sessionManager.getPersonalGameStatus("test", 1);
        
        assertNotNull(status);
        assertTrue(status instanceof CurrentGameStatusDTO);
        List<String> remainingWords = ((CurrentGameStatusDTO)status).getRemainingWords();
        assertEquals(12, remainingWords.size()); // 16 - 4
        assertFalse(remainingWords.contains("CAT"));
        assertTrue(remainingWords.contains("RED"));
    }
}
