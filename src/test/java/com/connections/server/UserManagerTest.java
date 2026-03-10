package com.connections.server;

import com.connections.server.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserManagerTest {

    @TempDir
    Path tempDir;

    private String persistenceFile;
    private UserManager userManager;

    @BeforeEach
    public void setup() {
        persistenceFile = tempDir.resolve("users.json").toString();
        userManager = new UserManager(persistenceFile);
    }

    @Test
    public void testRegistration() {
        assertTrue(userManager.register("alice", "pass123"));
        assertFalse(userManager.register("alice", "otherpass")); // Duplicate
    }

    @Test
    public void testAuthentication() {
        userManager.register("bob", "secret");
        User user = userManager.authenticate("bob", "secret");
        assertNotNull(user);
        assertEquals("bob", user.getUsername());
        
        assertNull(userManager.authenticate("bob", "wrong"));
        assertNull(userManager.authenticate("nobody", "secret"));
    }

    @Test
    public void testPersistence() {
        userManager.register("charlie", "pass");
        userManager.saveUserData();
        
        // Load again from same file
        UserManager otherManager = new UserManager(persistenceFile);
        assertNotNull(otherManager.authenticate("charlie", "pass"));
    }

    @Test
    public void testLeaderboardRanking() {
        userManager.register("u1", "p");
        userManager.register("u2", "p");
        userManager.register("u3", "p");
        
        // u2 has 10 points
        userManager.getUser("u2").recordWin(0, 10);
        // u1 has 20 points
        userManager.getUser("u1").recordWin(0, 20);
        // u3 has 5 points
        userManager.getUser("u3").recordWin(0, 5);
        
        List<User> top = userManager.getTopK(3);
        assertEquals(3, top.size());
        assertEquals("u1", top.get(0).getUsername());
        assertEquals("u2", top.get(1).getUsername());
        assertEquals("u3", top.get(2).getUsername());
    }
}
