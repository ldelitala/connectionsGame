package it.unipi.connections.server.engine;

import it.unipi.connections.common.models.Category;
import it.unipi.connections.common.protocol.Operation;
import it.unipi.connections.common.protocol.client_requests.GameplayRequest;
import it.unipi.connections.common.protocol.server_responses.ServerResponse;
import it.unipi.connections.common.protocol.server_responses.Status;
import it.unipi.connections.common.protocol.server_responses.dto.GuessResultDTO;
import it.unipi.connections.common.protocol.server_responses.dto.PuzzleStateDTO;
import it.unipi.connections.server.network.UdpNotifier;
import it.unipi.connections.server.models.Puzzle;
import it.unipi.connections.server.models.User;
import it.unipi.connections.server.persistence.PuzzlesDAO;
import it.unipi.connections.server.persistence.ServerStateDAO;
import it.unipi.connections.server.engine.HistoryCoordinator;
import it.unipi.connections.server.engine.UserCoordinator;
import it.unipi.connections.server.engine.PuzzleCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PuzzleCoordinatorTest {

    private PuzzlesDAO puzzlesDAO;
    private HistoryCoordinator historyCoordinator;
    private UserCoordinator userCoordinator;
    private UdpNotifier udpNotifier;
    private ServerStateDAO stateDAO;
    private PuzzleCoordinator coordinator;

    @BeforeEach
    public void setup() throws IOException {
        puzzlesDAO = mock(PuzzlesDAO.class);
        historyCoordinator = mock(HistoryCoordinator.class);
        userCoordinator = mock(UserCoordinator.class);
        udpNotifier = mock(UdpNotifier.class);
        stateDAO = mock(ServerStateDAO.class);

        // Mock a single puzzle
        Category group1 = new Category("Animals", Arrays.asList("CAT", "DOG", "FISH", "BIRD"));
        Category group2 = new Category("Colors", Arrays.asList("RED", "GREEN", "BLUE", "YELLOW"));
        Category group3 = new Category("Cities", Arrays.asList("LONDON", "PARIS", "ROME", "BERLIN"));
        Category group4 = new Category("Foods", Arrays.asList("PIZZA", "PASTA", "BURGER", "SUSHI"));
        Puzzle puzzle = new Puzzle(1, Arrays.asList(group1, group2, group3, group4));

        when(puzzlesDAO.getNextPuzzle()).thenReturn(puzzle);
        when(puzzlesDAO.getPuzzle(anyInt())).thenReturn(puzzle);

        coordinator = new PuzzleCoordinator(puzzlesDAO, userCoordinator, udpNotifier, stateDAO, 300000, 10000,
                                            (puzzleId, results) -> historyCoordinator.recordPuzzleResults(puzzleId, results));
        coordinator.loadNextPuzzle();
    }

    @Test
    public void testCorrectProposal() {
        User user = new User("test", "pass");
        when(userCoordinator.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "BIRD");
        GuessResultDTO result = coordinator.handleProposal("test", words);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals(6.0f, result.getPointsDelta());
        // Remaining words should be 12 (16 - 4)
        assertEquals(12, result.getRemainingWords().size());
    }

    @Test
    public void testWrongProposal() {
        User user = new User("test", "pass");
        when(userCoordinator.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "RED"); // Mixed
        GuessResultDTO result = coordinator.handleProposal("test", words);

        assertNotNull(result);
        assertEquals("wrong", result.getStatus());
        assertEquals(-4.0f, result.getPointsDelta());
        assertEquals(16, result.getRemainingWords().size());
    }

    @Test
    public void testWinningGame() {
        User user = new User("test", "pass");
        when(userCoordinator.getUser("test")).thenReturn(user);

        coordinator.handleProposal("test", Arrays.asList("CAT", "DOG", "FISH", "BIRD"));
        coordinator.handleProposal("test", Arrays.asList("RED", "GREEN", "BLUE", "YELLOW"));
        
        GuessResultDTO result = coordinator.handleProposal("test", Arrays.asList("LONDON", "PARIS", "ROME", "BERLIN"));

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        // Final points: 3 groups indovinati (+18) - 0 errori (0) = 18
        assertEquals(18, result.getTotalPoints());
    }

    @Test
    public void testGetPersonalStatusWithRemainingWords() {
        User user = new User("test", "pass");
        when(userCoordinator.getUser("test")).thenReturn(user);

        List<String> words = Arrays.asList("CAT", "DOG", "FISH", "BIRD");
        coordinator.handleProposal("test", words);

        PuzzleStateDTO status = coordinator.getPersonalStatus("test", 1);
        
        assertNotNull(status);
        assertFalse(status.isFinished());
        List<String> remainingWords = status.getRemainingWords();
        assertEquals(12, remainingWords.size());
        assertFalse(remainingWords.contains("CAT"));
        assertTrue(remainingWords.contains("RED"));
    }

    @Test
    public void testForbiddenDuringWait() {
        // Finalize current puzzle to enter waiting state
        coordinator.finalizePuzzle();
        
        // Try to get info during wait
        ServerResponse<?> response = coordinator.handle(GameplayRequest.getCurrentInfo(), Operation.GET_PUZZLE_INFO, "test");
        
        assertEquals(Status.FORBIDDEN, response.getStatus());
        assertTrue(response.getMessage().contains("Please wait"));
    }
}
