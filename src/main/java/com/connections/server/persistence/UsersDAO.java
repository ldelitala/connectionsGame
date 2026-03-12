package com.connections.server.persistence;

import com.connections.server.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Data Access Object for User accounts.
 * Manages thread-safe in-memory cache and persistent JSON storage.
 */
public class UsersDAO {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final String filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public UsersDAO(String filePath) {
        this.filePath = filePath;
        loadUserData();
    }

    private synchronized void loadUserData() {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("No user data found, starting with empty registry.");
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<ArrayList<User>>() {}.getType();
            List<User> userList = gson.fromJson(reader, type);
            if (userList != null) {
                for (User u : userList) {
                    users.put(u.getUsername(), u);
                }
                System.out.println("Loaded " + users.size() + " users.");
            }
        } catch (IOException e) {
            System.err.println("Error loading user data: " + e.getMessage());
        }
    }

    public synchronized void saveUserData() {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(users.values(), writer);
            System.out.println("User data saved successfully.");
        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
        }
    }

    public boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, new User(username, password));
        saveUserData();
        return true;
    }

    public User authenticate(String username, String password) {
        User u = users.get(username);
        if (u != null && u.getPassword().equals(password)) {
            return u;
        }
        return null;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public boolean updateCredentials(String oldName, String oldPsw, String newName, String newPsw) {
        User u = users.get(oldName);
        if (u == null || !u.getPassword().equals(oldPsw)) return false;

        if (newName != null && !newName.isEmpty() && !newName.equals(oldName)) {
            if (users.containsKey(newName)) return false;
            users.remove(oldName);
            u.setUsername(newName);
            users.put(newName, u);
        }

        if (newPsw != null && !newPsw.isEmpty()) {
            u.setPassword(newPsw);
        }

        saveUserData();
        return true;
    }
}
