package it.unipi.connections.server.persistence;

import it.unipi.connections.server.models.User;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user persistence using an append-only JSON strategy.
 * Efficiently indexes users by username for fast authentication and retrieval.
 */
public class UsersDAO {
    private final String filePath;
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UsersDAO(String filePath) {
        this.filePath = filePath;
        JsonPersistence.ensureDirectory(filePath);
        loadAll();
    }

    /**
     * Loads all users from disk into memory. Since the file is append-only,
     * later entries for the same user overwrite earlier ones.
     */
    private synchronized void loadAll() {
        if (!JsonPersistence.isReadable(filePath)) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                User u = JsonPersistence.GSON.fromJson(line, User.class);
                if (u != null) {
                    users.put(u.getUsername(), u);
                }
            }
            System.out.println("DAO: Loaded " + users.size() + " users.");
        } catch (IOException e) {
            System.err.println("DAO Error: Failed to load users: " + e.getMessage());
        }
    }

    /** Appends a single user state to the persistent log. */
    private synchronized boolean persist(User user) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(JsonPersistence.GSON.toJson(user));
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("DAO Error: Failed to persist user " + user.getUsername());
            return false;
        }
    }

    public boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        User newUser = new User(username, password);
        if (persist(newUser)) {
            users.put(username, newUser);
            return true;
        }
        return false;
    }

    public User authenticate(String username, String password) {
        User u = users.get(username);
        return (u != null && u.getPassword().equals(password)) ? u : null;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized boolean updateCredentials(String oldName, String oldPsw, String newName, String newPsw) {
        User u = users.get(oldName);
        if (u == null || !u.getPassword().equals(oldPsw)) return false;

        // If renaming, check if new name is taken
        if (newName != null && !newName.isEmpty() && !newName.equals(oldName)) {
            if (users.containsKey(newName)) return false;
            users.remove(oldName);
            u.setUsername(newName);
        }

        if (newPsw != null && !newPsw.isEmpty()) {
            u.setPassword(newPsw);
        }

        if (persist(u)) {
            users.put(u.getUsername(), u);
            return true;
        }
        return false;
    }

    public boolean saveUser(User u) {
        if (persist(u)) {
            users.put(u.getUsername(), u);
            return true;
        }
        return false;
    }
}
