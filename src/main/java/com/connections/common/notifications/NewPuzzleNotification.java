package com.connections.common.notifications;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO for UDP notifications sent when a new puzzle starts.
 */
public class NewPuzzleNotification extends Notification {

    /** The unique identifier for this notification type. */
    public static final String TYPE_ID = "new_game";

    @SerializedName("gameId")
    private final int puzzleId;
    private final List<String> words;

    private NewPuzzleNotification(int puzzleId, List<String> words) {
        super(TYPE_ID);
        this.puzzleId = puzzleId;
        this.words = words;
    }

    /**
     * Factory method to create a new puzzle notification.
     * 
     * @param puzzleId the ID of the new puzzle
     * @param words the 16 words for the puzzle
     * @return a new NewPuzzleNotification
     */
    public static NewPuzzleNotification create(int puzzleId, List<String> words) {
        return new NewPuzzleNotification(puzzleId, words);
    }

    public int getPuzzleId() { return puzzleId; }
    public List<String> getWords() { return words; }
}
