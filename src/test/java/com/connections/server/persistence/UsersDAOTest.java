package com.connections.server.persistence;

import com.connections.server.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class UsersDAOTest {

    @TempDir
    Path tempDir;

    private String persistenceFile;
    private UsersDAO usersDAO;

    @BeforeEach
    public void setup() {
        persistenceFile = tempDir.resolve("users.json").toString();
        usersDAO = new UsersDAO(persistenceFile);
    }

    @Test
    public void testRegistration() {
        assertTrue(usersDAO.register("alice", "pass123"));
        assertFalse(usersDAO.register("alice", "otherpass")); // Duplicate
    }

    @Test
    public void testAuthentication() {
        usersDAO.register("bob", "secret");
        User user = usersDAO.authenticate("bob", "secret");
        assertNotNull(user);
        assertEquals("bob", user.getUsername());
        
        assertNull(usersDAO.authenticate("bob", "wrong"));
        assertNull(usersDAO.authenticate("nobody", "secret"));
    }

    @Test
    public void testPersistence() {
        usersDAO.register("charlie", "pass");
        usersDAO.saveUserData();
        
        // Load again from same file
        UsersDAO otherDAO = new UsersDAO(persistenceFile);
        assertNotNull(otherDAO.authenticate("charlie", "pass"));
    }
}
