package com.connections.client.modules;

import com.connections.common.protocol.client_requests.*;
import com.connections.common.protocol.server_responses.*;
import java.util.Scanner;

/**
 * Handles account-related request creation and response display.
 */
public class UserModule {
    private final Scanner scanner;
    private final int udpPort;

    public UserModule(Scanner scanner, int udpPort) {
        this.scanner = scanner;
        this.udpPort = udpPort;
    }

    public ClientRequest prepareRegister(String[] tokens) {
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

    public ClientRequest prepareLogin(String[] tokens) {
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

    public ClientRequest prepareLogout() {
        return UserRequest.logout();
    }

    public ClientRequest prepareUpdate(String currentUser) {
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

    public void displayResponse(ServerResponse<?> response) {
        System.out.println(response.getMessage());
    }
}
