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
        
        // Load again from same file
        UsersDAO otherDAO = new UsersDAO(persistenceFile);
        assertNotNull(otherDAO.authenticate("charlie", "pass"));
    }

    @Test
    public void testStatsPersistence() {
        usersDAO.register("dan", "pass");
        User user = usersDAO.getUser("dan");
        assertNotNull(user);
        
        user.recordWin(0, 10);
        usersDAO.saveUser(user);
        
        // Reload and check stats
        UsersDAO otherDAO = new UsersDAO(persistenceFile);
        User reloaded = otherDAO.getUser("dan");
        assertNotNull(reloaded);
        assertEquals(1, reloaded.getWins());
        assertEquals(10, reloaded.getTotalScore());
    }

    @Test
    public void testUpdateCredentials() {
        usersDAO.register("emma", "pass1");
        assertTrue(usersDAO.updateCredentials("emma", "pass1", "emma_new", "pass2"));
        
        assertNull(usersDAO.authenticate("emma", "pass1"));
        assertNotNull(usersDAO.authenticate("emma_new", "pass2"));
    }
}
