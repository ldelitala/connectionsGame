package com.connections.client.controller_state;

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

    // --- Commands ---
    private static final String CMD_LOGOUT = "logout";
    private static final String CMD_UPDATE = "update";
    private static final String CMD_SUBMIT = "submit";
    private static final String CMD_STATUS = "status";
    private static final String CMD_STATS = "stats";
    private static final String CMD_INFO = "info";
    private static final String CMD_GLOBAL = "global";
    private static final String CMD_RANKING = "ranking";
    private static final String CMD_HISTORY = "history";
    private static final String CMD_REGISTER = "register";
    private static final String CMD_LOGIN = "login";
    private static final String CMD_HELP = "help";

    // --- Formatting ---
    private static final String PROMPT_FORMAT = "[%s] > ";

    // --- Help UI ---
    private static final String HELP_HEADER = "\n--- AVAILABLE COMMANDS (Logged In) ---";
    private static final String HELP_SECTION_ACTIVE = "Active Game:";
    private static final String HELP_SUBMIT = "  submit [w1-w4]    : Send a 4-word proposal";
    private static final String HELP_STATUS = "  status            : View your current game status (mistakes, time)";
    private static final String HELP_STATS = "  stats             : View global participation stats for current game";
    private static final String HELP_SECTION_HISTORY = "\nHistory and Rankings:";
    private static final String HELP_INFO = "  info [id]         : View details and solution of a past game";
    private static final String HELP_GLOBAL = "  global [id]       : View global statistics of a past game";
    private static final String HELP_RANKING = "  ranking [k] [u]   : View top players or a specific user's rank";
    private static final String HELP_HISTORY = "  history           : View your personal overall statistics";
    private static final String HELP_SECTION_ACCOUNT = "\nAccount:";
    private static final String HELP_UPDATE = "  update            : Change your username or password";
    private static final String HELP_LOGOUT = "  logout            : Sign out of your account";
    private static final String HELP_SECTION_SYSTEM = "\nSystem:";
    private static final String HELP_HELP = "  help              : Show this list";
    private static final String HELP_EXIT = "  exit              : Close the application\n";

    private final Controller controller;
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

            case CMD_HELP:
                printHelp();
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

    @Override
    public void printHelp() {
        System.out.println(HELP_HEADER);
        System.out.println(HELP_SECTION_ACTIVE);
        System.out.println(HELP_SUBMIT);
        System.out.println(HELP_STATUS);
        System.out.println(HELP_STATS);

        System.out.println(HELP_SECTION_HISTORY);
        System.out.println(HELP_INFO);
        System.out.println(HELP_GLOBAL);
        System.out.println(HELP_RANKING);
        System.out.println(HELP_HISTORY);

        System.out.println(HELP_SECTION_ACCOUNT);
        System.out.println(HELP_UPDATE);
        System.out.println(HELP_LOGOUT);

        System.out.println(HELP_SECTION_SYSTEM);
        System.out.println(HELP_HELP);
        System.out.println(HELP_EXIT);
    }

    @Override
    public String getPrompt() {
        return String.format(PROMPT_FORMAT, username);
    }
}
