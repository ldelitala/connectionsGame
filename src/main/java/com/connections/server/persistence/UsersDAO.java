package com.connections.server.persistence;

import com.connections.server.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance Data Access Object for User accounts.
 * Uses byte-offset indexing and append-only storage for thread-safe, 
 * low-RAM persistent storage.
 */
public class UsersDAO {
    private final String filePath;
    private final Map<String, Long> userOffsets = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().create();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");

    public UsersDAO(String filePath) {
        this.filePath = filePath;
        try {
            ensureFileExists();
            buildIndex();
        } catch (IOException e) {
            System.err.println("Critical error initializing UsersDAO: " + e.getMessage());
        }
    }

    private void ensureFileExists() throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    /**
     * Scans the file and indexes the LATEST byte offset for each username.
     */
    private synchronized void buildIndex() throws IOException {
        userOffsets.clear();
        File file = new File(filePath);
        if (file.length() == 0) return;

        try (PushbackInputStream pis = new PushbackInputStream(new BufferedInputStream(new FileInputStream(filePath)), 1)) {
            long currentOffset = 0;
            int b;

            while ((b = pis.read()) != -1) {
                if (b == '{') {
                    pis.unread(b);
                    long startOffset = currentOffset;
                    ExtractedObject result = extractObject(pis);
                    String jsonUser = result.json;
                    
                    String username = extractUsername(jsonUser);
                    if (username != null) {
                        // Later entries for the same user overwrite earlier ones in the index
                        userOffsets.put(username, startOffset);
                    }

                    currentOffset += result.bytesRead;
                } else {
                    currentOffset++;
                }
            }
        }
        System.out.println("Indexed " + userOffsets.size() + " unique users.");
    }

    private ExtractedObject extractObject(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int balance = 0;
        int b;
        boolean started = false;

        while ((b = is.read()) != -1) {
            baos.write(b);
            char c = (char) b;

            if (c == '{') {
                balance++;
                started = true;
            } else if (c == '}') {
                balance--;
            }
            
            if (started && balance == 0) break;
        }
        return new ExtractedObject(baos.toString("UTF-8"), baos.size());
    }

    private static class ExtractedObject {
        final String json;
        final int bytesRead;
        ExtractedObject(String json, int bytesRead) {
            this.json = json;
            this.bytesRead = bytesRead;
        }
    }

    private String extractUsername(String json) {
        Matcher matcher = USERNAME_PATTERN.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public synchronized boolean register(String username, String password) {
        if (userOffsets.containsKey(username)) return false;
        
        User newUser = new User(username, password);
        return appendUser(newUser);
    }

    /**
     * Appends a user object to the end of the file and updates the index.
     */
    private synchronized boolean appendUser(User user) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            long offset = raf.length();
            raf.seek(offset);
            
            String json = gson.toJson(user) + "\n";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            raf.write(bytes);
            
            userOffsets.put(user.getUsername(), offset);
            return true;
        } catch (IOException e) {
            System.err.println("Error appending user to disk: " + e.getMessage());
            return false;
        }
    }

    public User authenticate(String username, String password) {
        User u = getUser(username);
        if (u != null && u.getPassword().equals(password)) {
            return u;
        }
        return null;
    }

    public User getUser(String username) {
        Long offset = userOffsets.get(username);
        if (offset == null) return null;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            fis.getChannel().position(offset);
            ExtractedObject result = extractObject(fis);
            return gson.fromJson(result.json, User.class);
        } catch (IOException e) {
            System.err.println("Error reading user " + username + " from disk: " + e.getMessage());
        }
        return null;
    }

    public synchronized List<User> getAllUsers() {
        List<User> allUsers = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            for (Long offset : userOffsets.values()) {
                fis.getChannel().position(offset);
                ExtractedObject result = extractObject(fis);
                User u = gson.fromJson(result.json, User.class);
                if (u != null) allUsers.add(u);
            }
        } catch (IOException e) {
            System.err.println("Error batch reading users from disk: " + e.getMessage());
        }
        return allUsers;
    }

    /**
     * Updates an existing user's credentials.
     * Appends the new version to the file and updates the index.
     */
    public synchronized boolean updateCredentials(String oldName, String oldPsw, String newName, String newPsw) {
        User u = getUser(oldName);
        if (u == null || !u.getPassword().equals(oldPsw)) return false;

        boolean nameChanged = false;
        if (newName != null && !newName.isEmpty() && !newName.equals(oldName)) {
            if (userOffsets.containsKey(newName)) return false;
            userOffsets.remove(oldName);
            u.setUsername(newName);
            nameChanged = true;
        }

        if (newPsw != null && !newPsw.isEmpty()) {
            u.setPassword(newPsw);
        }

        boolean success = appendUser(u);
        if (success && nameChanged) {
            // Index already updated by appendUser with newName
        }
        return success;
    }

    /**
     * Persists an updated user object (e.g. after stats update).
     */
    public synchronized boolean saveUser(User u) {
        if (!userOffsets.containsKey(u.getUsername())) return false;
        return appendUser(u);
    }
}
