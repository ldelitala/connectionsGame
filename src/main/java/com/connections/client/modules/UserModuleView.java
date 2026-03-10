package com.connections.client.modules;

import com.connections.common.protocol.client_requests.ClientRequest;
import com.connections.common.protocol.client_requests.UserRequest;
import com.connections.common.protocol.server_responses.ServerResponse;
import java.util.Scanner;

/**
 * Handles account-related request creation and response display.
 */
public class UserModuleView implements ModuleView {

    // --- Prompts ---
    private static final String PROMPT_OLD_PASSWORD = "Enter current password (required): ";
    private static final String PROMPT_NEW_USERNAME = "Enter new username (leave empty to skip): ";
    private static final String PROMPT_NEW_PASSWORD = "Enter new password (leave empty to skip): ";

    // --- Indices ---
    private static final int INDEX_USERNAME = 1;
    private static final int INDEX_PASSWORD = 2;
    private static final int MIN_TOKENS_AUTH = 3;

    private final Scanner scanner;
    private final int udpPort;

    public UserModuleView(Scanner scanner, int udpPort) {
        this.scanner = scanner;
        this.udpPort = udpPort;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public ClientRequest prepareRegister(String[] tokens) {
        String user, pass;
        if (tokens.length >= MIN_TOKENS_AUTH) {
            user = tokens[INDEX_USERNAME];
            pass = tokens[INDEX_PASSWORD];
        } else {
            System.out.println("Usage: register <username> <password>");
            return null;
        }
        return UserRequest.register(user, pass);
    }

    public ClientRequest prepareLogin(String[] tokens) {
        String user, pass;
        if (tokens.length >= MIN_TOKENS_AUTH) {
            user = tokens[INDEX_USERNAME];
            pass = tokens[INDEX_PASSWORD];
        } else {
            System.out.println("Usage: login <username> <password>");
            return null;
        }
        return UserRequest.login(user, pass, udpPort);
    }

    public ClientRequest prepareLogout() {
        return UserRequest.logout();
    }

    public ClientRequest prepareUpdate(String currentUsername) {
        System.out.print(PROMPT_OLD_PASSWORD);
        String oldPassword = scanner.nextLine().trim();
        if (oldPassword.isEmpty()) {
            System.out.println("Old password is required to perform updates.");
            return null;
        }

        System.out.print(PROMPT_NEW_USERNAME);
        String newUsername = scanner.nextLine().trim();
        if (newUsername.isEmpty()) {
            newUsername = null;
        }

        System.out.print(PROMPT_NEW_PASSWORD);
        String newPassword = scanner.nextLine().trim();
        if (newPassword.isEmpty()) {
            newPassword = null;
        }

        if (newUsername == null && newPassword == null) {
            System.out.println("No changes provided.");
            return null;
        }

        // The protocol requires oldName (currentUsername) and oldPsw to authorize the change.
        return UserRequest.updateCredentials(currentUsername, oldPassword, newUsername, newPassword);
    }

    @Override
    public void displayResponse(ServerResponse<?> response) {
        System.out.println("[" + response.getStatus() + "] " + response.getMessage());
    }
}
