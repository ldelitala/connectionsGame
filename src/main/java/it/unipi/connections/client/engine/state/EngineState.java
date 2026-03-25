package it.unipi.connections.client.engine.state;

import java.util.List;

/**
 * Interface for Game Engine states. Defines how the engine reacts to specific user actions in
 * different contexts (LoggedOut vs LoggedIn).
 */
public interface EngineState {

    // Auth actions
    void onRegister(String username, String password);

    void onLogin(String username, String password);

    void onLogout();

    void onUpdateCredentials(String oldPass, String newName, String newPass);

    // Gameplay actions
    void onGuess(List<String> words);

    void onRequestBoard();

    void onRequestStats();

    // Archive actions
    void onRequestPastInfo(int puzzleId);

    void onRequestGlobalStats(int puzzleId);

    void onRequestRanking(String username, Integer topK);

    void onRequestHistory();

    // General
    String getPrompt();

    void onExit();

    void onHelp();
}
