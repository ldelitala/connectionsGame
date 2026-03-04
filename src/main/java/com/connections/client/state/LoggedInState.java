package com.connections.client.state;

import com.connections.client.Controller;
import com.connections.client.modules.ModuleView;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * Represents the client state when a user is successfully authenticated (Logged In). In this state,
 * the user can participate in the active game, view historical data, check global rankings, and
 * manage their account. Implements the State pattern to manage contextual client behavior.
 */
public class LoggedInState implements ControllerState {

    /** The main controller acting as the context for the state. */
    private final Controller controller;

    /** The username of the currently logged-in user. */
    private final String username;

    /**
     * Constructs a new LoggedInState.
     * 
     * @param controller the controller managing the client's state machine.
     * @param username the name of the authenticated user.
     */
    public LoggedInState(Controller controller, String username) {
        this.controller = controller;
        this.username = username;
    }

    /**
     * Handles user commands by delegating request creation to the appropriate modules, then sending
     * them to the server and last displaying the response. Commands are categorized into active
     * gameplay, historical data, and account management. The method also manages state transitions
     * (e.g., logout) based on server responses.
     * 
     * @param tokens the array of strings composing the command entered by the user.
     */
    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;
        ModuleView activeModule = null;
        boolean trylogout = false;

        switch (command) {

            // --- ACCOUNT MANAGEMENT ---
            case "logout":
                request = controller.getUserModule().prepareLogout();
                activeModule = controller.getUserModule();
                trylogout = true;
                break;
            case "update":
                request = controller.getUserModule().prepareUpdate(username);
                activeModule = controller.getUserModule();
                break;

            // --- ACTIVE GAME ---
            case "submit":
                request = controller.getGameModule().prepareSubmit(tokens);
                activeModule = controller.getGameModule();
                break;
            case "status":
                request = controller.getGameModule().prepareStatus();
                activeModule = controller.getGameModule();
                break;
            case "stats":
                request = controller.getGameModule().prepareStats();
                activeModule = controller.getGameModule();
                break;

            // --- HISTORY AND GLOBAL DATA ---
            case "info":
                request = controller.getHistoryModule().preparePastInfo(tokens);
                activeModule = controller.getHistoryModule();
                break;
            case "global":
                request = controller.getHistoryModule().prepareGlobalStats(tokens);
                activeModule = controller.getHistoryModule();
                break;
            case "ranking":
                request = controller.getHistoryModule().prepareRanking(tokens);
                activeModule = controller.getHistoryModule();
                break;
            case "history":
                request = controller.getHistoryModule().prepareHistory();
                activeModule = controller.getHistoryModule();
                break;

            // --- ALREADY LOGGED IN ---
            case "register":
            case "login":
                System.out.println("Already logged in as [" + username + "]. Please logout first.");
                return;

            default:
                System.out.println("Unknown command. Type 'help' for assistance.");
                return;
        }

        if (request != null) {
            try {
                ServerResponse<?> response = controller.getTcpPort().sendRequest(request);
                activeModule.displayResponse(response);

                if (trylogout && response.getStatus() == Status.OK) {
                    controller.changeState(new LoggedOutState(controller));
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }


    /**
     * Prints contextual help for the logged-in state. Lists commands for active gameplay,
     * historical data, and account management.
     */
    @Override
    public void printHelp() {
        System.out.println("\n--- AVAILABLE COMMANDS (Logged In) ---");
        System.out.println("Active Game:");
        System.out.println("  submit [w1-w4]    : Send a 4-word proposal");
        System.out.println("  status            : View your current game status (mistakes, time)");
        System.out
                .println("  stats             : View global participation stats for current game");

        System.out.println("\nHistory and Rankings:");
        System.out.println("  info [id]         : View details and solution of a past game");
        System.out.println("  global [id]       : View global statistics of a past game");
        System.out.println("  ranking [k] [u]   : View top players or a specific user's rank");
        System.out.println("  history           : View your personal overall statistics");

        System.out.println("\nAccount:");
        System.out.println("  update            : Change your username or password");
        System.out.println("  logout            : Sign out of your account");

        System.out.println("\nSystem:");
        System.out.println("  help              : Show this list");
        System.out.println("  exit              : Close the application\n");
    }

    /**
     * Returns the dynamic command prompt for the authenticated user.
     * 
     * @return the prompt string formatted as "[username] > ".
     */
    @Override
    public String getPrompt() {
        return "[" + username + "] > ";
    }
}
