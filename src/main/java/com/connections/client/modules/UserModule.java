package com.connections.client.modules;

import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import java.util.Scanner;

/**
 * Handles user account operations (register, login, logout, update).
 */
public class UserModule implements ClientModule {
    private final Scanner scanner;
    private final int udpPort;

    public UserModule(Scanner scanner, int udpPort) {
        this.scanner = scanner;
        this.udpPort = udpPort;
    }

    @Override
    public Request parseCommand(String command, String[] tokens) {
        switch (command.toLowerCase()) {
            case "register":
                return handleRegister(tokens);
            case "login":
                return handleLogin(tokens);
            case "logout":
                return UserRequest.logout();
            case "update":
                return handleUpdate();
            default:
                return null;
        }
    }

    private UserRequest handleRegister(String[] tokens) {
        String user, pass;
        if (tokens.length >= 3) {
            user = tokens[1];
            pass = tokens[2];
        } else {
            System.out.print("Username: ");
            user = scanner.nextLine().trim();
            System.out.print("Password: ");
            pass = scanner.nextLine().trim();
        }
        return (user.isEmpty() || pass.isEmpty()) ? null : UserRequest.register(user, pass);
    }

    private UserRequest handleLogin(String[] tokens) {
        String user, pass;
        if (tokens.length >= 3) {
            user = tokens[1];
            pass = tokens[2];
        } else {
            System.out.print("Username: ");
            user = scanner.nextLine().trim();
            System.out.print("Password: ");
            pass = scanner.nextLine().trim();
        }
        return (user.isEmpty() || pass.isEmpty()) ? null : UserRequest.login(user, pass, udpPort);
    }

    private UserRequest handleUpdate() {
        System.out.print("New username (leave empty to keep current): ");
        String newUname = scanner.nextLine().trim();
        if (newUname.isEmpty()) newUname = null;
        
        System.out.print("Current password: ");
        String oldPass = scanner.nextLine().trim();
        System.out.print("New password (leave empty to keep current): ");
        String newPass = scanner.nextLine().trim();
        if (newPass.isEmpty()) newPass = null;

        return UserRequest.updateCredentials(null, oldPass, newUname, newPass);
    }

    @Override
    public void displayResponse(Response<?> response) {
        System.out.println(response.getMessage());
    }
}
