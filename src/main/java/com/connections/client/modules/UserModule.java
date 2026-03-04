package com.connections.client.modules;

import com.connections.common.protocol.requests.*;
import com.connections.common.protocol.responses.*;
import java.util.Scanner;

/**
 * Handles account-related requests and responses (register, login, logout, update).
 */
public class UserModule {
    private final Scanner scanner;
    private final int udpPort;
    private String currentUser;

    public UserModule(Scanner scanner, int udpPort) {
        this.scanner = scanner;
        this.udpPort = udpPort;
    }

    public void setCurrentUser(String user) {
        this.currentUser = user;
    }

    public Request prepareRegister(String[] tokens) {
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

    public Request prepareLogin(String[] tokens) {
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

    public Request prepareLogout() {
        return UserRequest.logout();
    }

    public Request prepareUpdate() {
        if (currentUser == null) {
            System.out.println("Devi essere loggato per cambiare le credenziali.");
            return null;
        }
        System.out.print("Nuovo username (vuoto per mantenere [" + currentUser + "]): ");
        String newUname = scanner.nextLine().trim();
        if (newUname.isEmpty()) newUname = null;
        
        System.out.print("Password attuale: ");
        String oldPass = scanner.nextLine().trim();
        System.out.print("Nuova password (vuoto per non cambiarla): ");
        String newPass = scanner.nextLine().trim();
        if (newPass.isEmpty()) newPass = null;

        return UserRequest.updateCredentials(currentUser, oldPass, newUname, newPass);
    }

    public void displayResponse(Response<?> response) {
        System.out.println(response.getMessage());
    }
}
