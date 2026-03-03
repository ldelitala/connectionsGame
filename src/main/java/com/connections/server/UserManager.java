package com.connections.server;

import com.connections.server.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages user registration, login, and persistence of user data.
 */
public class UserManager {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final String persistenceFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public UserManager(String persistenceFile) {
        this.persistenceFile = persistenceFile;
        loadUsers();
    }

    private void loadUsers() {
        try (FileReader reader = new FileReader(persistenceFile)) {
            Type type = new TypeToken<Map<String, User>>(){}.getType();
            Map<String, User> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                users.putAll(loaded);
            }
            System.out.println("Loaded " + users.size() + " users.");
        } catch (IOException e) {
            System.out.println("No user data found, starting with empty registry.");
        }
    }

    public synchronized void saveUsers() {
        try (FileWriter writer = new FileWriter(persistenceFile)) {
            gson.toJson(users, writer);
            System.out.println("User data saved successfully.");
        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
        }
    }

    public synchronized boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, new User(username, password));
        saveUsers();
        return true;
    }

    public User authenticate(String username, String password) {
        User user = users.get(username);
        if (user != null && user.checkPassword(password)) {
            return user;
        }
        return null;
    }

    public synchronized boolean updateCredentials(String oldUsername, String newUsername, String oldPassword, String newPassword) {
        User user = users.get(oldUsername);
        if (user == null || !user.checkPassword(oldPassword)) return false;
        
        if (newUsername != null && !newUsername.equals(oldUsername)) {
            if (users.containsKey(newUsername)) return false;
            users.remove(oldUsername);
            user.setUsername(newUsername);
            users.put(newUsername, user);
        }
        
        if (newPassword != null) {
            user.updatePassword(newPassword);
        }
        
        saveUsers();
        return true;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public List<User> getTopK(int k) {
        return users.values().stream()
                .sorted((u1, u2) -> Integer.compare(u2.getTotalScore(), u1.getTotalScore()))
                .limit(k)
                .collect(Collectors.toList());
    }

    public List<User> getAllUsers() {
        return users.values().stream()
                .sorted((u1, u2) -> Integer.compare(u2.getTotalScore(), u1.getTotalScore()))
                .collect(Collectors.toList());
    }
}
