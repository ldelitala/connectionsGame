package it.unipi.connections.common.protocol.client_requests;

import it.unipi.connections.common.protocol.Operation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Specialization of {@link ClientRequest} for operations related to active
 * gameplay sessions. This
 * class handles word group submissions, retrieval of current puzzle
 * information, and current game
 * statistics.
 * <p>
 * The puzzle context is implicitly defined by the user's current session.
 * </p>
 */
public class GameplayRequest extends ClientRequest {

    private static final EnumSet<Operation> ALLOWED_OPERATIONS = Operation.gameplayOps();

    private final List<String> words;

    private GameplayRequest(Operation op, List<String> words) {
        super(op);
        this.words = (words != null) ? Collections.unmodifiableList(new ArrayList<>(words)) : null;
    }

    // --------------------------------------------------------------------------
    // Factory Methods
    // --------------------------------------------------------------------------

    public static GameplayRequest submitProposal(List<String> words) {
        return new GameplayRequest(Operation.SUBMIT_PROPOSAL, words);
    }

    public static GameplayRequest getCurrentInfo() {
        return new GameplayRequest(Operation.GET_PUZZLE_INFO, null);
    }

    public static GameplayRequest getCurrentStats() {
        return new GameplayRequest(Operation.GET_PUZZLE_STATS, null);
    }

    // --------------------------------------------------------------------------

    public List<String> getWords() {
        return words;
    }

    @Override
    public boolean isValid() {
        Operation op = getOperation();

        if (!ALLOWED_OPERATIONS.contains(op)) {
            return false;
        }

        if (op == Operation.SUBMIT_PROPOSAL) {
            return words != null && !words.isEmpty() && words.size() == 4;
        }

        return true;
    }
}
