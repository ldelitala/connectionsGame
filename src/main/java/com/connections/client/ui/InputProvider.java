package com.connections.client.ui;

import java.util.List;

/**
 * Interface for a source of user inputs.
 * Acting as an Adapter between external events (CLI, GUI) and the Game Engine actions.
 */
public interface InputProvider extends AutoCloseable {
    
    void startListening(InputListener listener);
    
    void stop();

    @Override
    void close();

    /**
     * Listener interface defining all possible actions the Game Engine can perform.
     * This acts as the explicit contract between the UI and the Engine.
     */
    interface InputListener {
        // Authentication & Account
        void onRegister(String username, String password);
        void onLogin(String username, String password);
        void onLogout();
        void onUpdateCredentials(String oldPass, String newName, String newPass);

        // Gameplay
        void onGuess(List<String> words);
        void onRequestBoard();
        void onRequestStats();

        // History & Rankings
        void onRequestPastInfo(int puzzleId);
        void onRequestGlobalStats(int puzzleId);
        void onRequestRanking(String username, Integer topK);
        void onRequestHistory();

        // System
        void onHelp();
        void onExit();
    }
}
