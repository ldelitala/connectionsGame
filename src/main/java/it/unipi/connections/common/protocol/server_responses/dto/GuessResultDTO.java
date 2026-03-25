package it.unipi.connections.common.protocol.server_responses.dto;

import java.util.List;

/**
 * DTO for the result of a word proposal. Used in the response to SUBMIT_PROPOSAL operation.
 * Contains the status of the proposal, points awarded/deducted, total points after the proposal,
 * and the list of remaining words to discover. This DTO is crucial for providing immediate feedback
 * to players on their guesses and guiding their next moves. The 'status' field can be "success",
 * "wrong", or other custom statuses defined by the server logic.
 */
public class GuessResultDTO implements DTO {
    private final String status;
    private final float pointsDelta;
    private final Integer totalPoints;
    private final List<String> remainingWords;

    private GuessResultDTO(String status, float pointsDelta, Integer totalPoints,
            List<String> remainingWords) {
        this.status = status;
        this.pointsDelta = pointsDelta;
        this.totalPoints = totalPoints;
        this.remainingWords = remainingWords;
    }

    /** Factory for success proposal. */
    public static GuessResultDTO success(float pointsDelta, Integer totalPoints,
            List<String> remainingWords) {
        return new GuessResultDTO("success", pointsDelta, totalPoints, remainingWords);
    }

    /** Factory for wrong proposal. */
    public static GuessResultDTO wrong(float pointsDelta, Integer totalPoints,
            List<String> remainingWords) {
        return new GuessResultDTO("wrong", pointsDelta, totalPoints, remainingWords);
    }

    /** Factory for generic proposal result. */
    public static GuessResultDTO create(String status, double pointsDelta, int totalPoints,
            List<String> remainingWords) {
        return new GuessResultDTO(status, (float) pointsDelta, totalPoints, remainingWords);
    }

    public String getStatus() {
        return status;
    }

    public float getPointsDelta() {
        return pointsDelta;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public List<String> getRemainingWords() {
        return remainingWords;
    }
}
