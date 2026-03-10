package com.connections.client.controller_state;

import com.connections.client.Controller;
import com.connections.client.modules.ModuleView;
import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import com.connections.common.protocol.server_responses.Status;

/**
 * Represents the client state when a user is successfully authenticated (Logged In).
 * In this state, the user can participate in active games, view history, and manage their account.
 */
public class LoggedInState implements ControllerState {

    // --- Commands: Account ---
    private static final String CMD_LOGOUT = "logout";
    private static final String CMD_UPDATE = "update";

    // --- Commands: Active Game ---
    private static final String CMD_SUBMIT = "submit";
    private static final String CMD_STATUS = "status";
    private static final String CMD_STATS = "stats";

    // --- Commands: History and Global ---
    private static final String CMD_INFO = "info";
    private static final String CMD_GLOBAL = "global";
    private static final String CMD_RANKING = "ranking";
    private static final String CMD_HISTORY = "history";

    // --- System / State Internal Commands ---
    private static final String CMD_REGISTER = "register";
    private static final String CMD_LOGIN = "login";

    // --- Messages ---
    private static final String HELP_MESSAGE = 
        "\n--- AVAILABLE COMMANDS (Logged In) ---\n" +
        "Active Game:\n" +
        "  submit [w1-w4]    : Send a 4-word proposal\n" +
        "  status            : View your current game status (mistakes, time)\n" +
        "  stats             : View global participation stats for current game\n" +
        "\nHistory and Rankings:\n" +
        "  info [id]         : View details and solution of a past game\n" +
        "  global [id]       : View global statistics of a past game\n" +
        "  ranking [k] [u]   : View top players or a specific user's rank\n" +
        "  history           : View your personal overall statistics\n" +
        "\nAccount:\n" +
        "  update            : Change your username or password\n" +
        "  logout            : Sign out of your account\n";

    // --- Formatting ---
    private static final String PROMPT_FORMAT = "[%s] > ";

    /** The main controller acting as the context for the state. */
    private final Controller controller;

    /** The username of the currently logged-in user. */
    private final String username;

    /**
     * Constructs a new LoggedInState for the given user.
     * 
     * @param controller the controller managing the client's state machine.
     * @param username the name of the authenticated user.
     */
    public LoggedInState(Controller controller, String username) {
        this.controller = controller;
        this.username = username;
    }

    /**
     * Handles user commands by delegating request creation to modules and 
     * interacting with the server via the controller's TCP port.
     * 
     * @param tokens the command split into tokens.
     */
    @Override
    public void handleCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        ClientRequest request = null;
        ModuleView activeModule = null;
        boolean trylogout = false;

        switch (command) {

            // --- ACCOUNT MANAGEMENT ---
            case CMD_LOGOUT:
                request = controller.getUserModule().prepareLogout();
                activeModule = controller.getUserModule();
                trylogout = true;
                break;
            case CMD_UPDATE:
                request = controller.getUserModule().prepareUpdate(username);
                activeModule = controller.getUserModule();
                break;

            // --- ACTIVE GAME ---
            case CMD_SUBMIT:
                request = controller.getGameModule().prepareSubmit(tokens);
                activeModule = controller.getGameModule();
                break;
            case CMD_STATUS:
                request = controller.getGameModule().prepareStatus();
                activeModule = controller.getGameModule();
                break;
            case CMD_STATS:
                request = controller.getGameModule().prepareStats();
                activeModule = controller.getGameModule();
                break;

            // --- HISTORY AND GLOBAL DATA ---
            case CMD_INFO:
                request = controller.getHistoryModule().preparePastInfo(tokens);
                activeModule = controller.getHistoryModule();
                break;
            case CMD_GLOBAL:
                request = controller.getHistoryModule().prepareGlobalStats(tokens);
                activeModule = controller.getHistoryModule();
                break;
            case CMD_RANKING:
                request = controller.getHistoryModule().prepareRanking(tokens);
                activeModule = controller.getHistoryModule();
                break;
            case CMD_HISTORY:
                request = controller.getHistoryModule().prepareHistory();
                activeModule = controller.getHistoryModule();
                break;

            // --- ALREADY LOGGED IN ---
            case CMD_REGISTER:
            case CMD_LOGIN:
                System.out.println(String.format("Already logged in as [%s]. Please logout first.", username));
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
     * Prints help documentation specific to the logged-in state.
     */
    @Override
    public void printHelp() {
        System.out.println(HELP_MESSAGE);
    }

    /**
     * Returns the dynamic command prompt for the authenticated user.
     * 
     * @return the prompt string formatted with the username.
     */
    @Override
    public String getPrompt() {
        return String.format(PROMPT_FORMAT, username);
    }

    @Override
    public void onExit() {
        try {
            ClientRequest request = controller.getUserModule().prepareLogout();
            controller.getTcpPort().sendRequest(request);
        } catch (Exception ignored) {
            // Silently fail as we are closing the application anyway
        }
    }
}
