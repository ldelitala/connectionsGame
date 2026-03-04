package com.connections.client.modules;

import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import java.util.Scanner;

/**
 * Handles account-related commands (register, login, logout, update).
 */
public class UserModule implements ClientModule {
    private final Scanner scanner;
    private final int udpPort;
    private String currentUser; // Passato dal ClientController

    public UserModule(Scanner scanner, int udpPort) {
        this.scanner = scanner;
        this.udpPort = udpPort;
    }

    public void setCurrentUser(String user) {
        this.currentUser = user;
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
            System.out.print("Username desiderato: ");
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
        if (currentUser == null) {
            System.out.println("Devi essere loggato per cambiare le credenziali.");
            return null;
        }

        System.out.print("Nuovo username (lascia vuoto per mantenere [" + currentUser + "]): ");
        String newUname = scanner.nextLine().trim();
        if (newUname.isEmpty()) newUname = null;
        
        System.out.print("Password attuale: ");
        String oldPass = scanner.nextLine().trim();
        System.out.print("Nuova password (lascia vuoto per non cambiarla): ");
        String newPass = scanner.nextLine().trim();
        if (newPass.isEmpty()) newPass = null;

        return UserRequest.updateCredentials(currentUser, oldPass, newUname, newPass);
    }

    @Override
    public void displayResponse(Response<?> response) {
        System.out.println(response.getMessage());
    }
}
