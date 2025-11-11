package com.group5.smartattendance.user;

import com.group5.smartattendance.persistence.UserDatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    // @BeforeEach
    // void setUp() throws Exception {
    // UserDatabaseManager.deleteDatabase();
    // UserDatabaseManager.initialize();
    // }

    // @AfterEach
    // void tearDown() throws Exception {
    // UserDatabaseManager.deleteDatabase();
    // }

    @Test
    void testSignupAndLogin() throws Exception {
        // Test signup
        String email = "test@example.com";
        String fullName = "Test User";
        String password = "testpass123";

        User user = UserService.signup(email, fullName, password);
        assertNotNull(user);
        assertEquals(email, user.getEmail());
        assertEquals(fullName, user.getFullName());
        assertNotNull(user.getPasswordHash());

        // Test login
        User loggedInUser = UserService.login(email, password);
        assertNotNull(loggedInUser);
        assertEquals(email, loggedInUser.getEmail());
        assertEquals(fullName, loggedInUser.getFullName());
    }

    @Test
    void testSignupDuplicateEmail() throws Exception {
        String email = "test@example.com";
        String fullName1 = "Test User 1";
        String fullName2 = "Test User 2";
        String password1 = "password1";
        String password2 = "password2";

        UserService.signup(email, fullName1, password1);

        assertThrows(Exception.class, () -> {
            UserService.signup(email, fullName2, password2);
        });
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        String email = "test@example.com";
        String fullName = "Test User";
        String password = "testpass123";

        // Try to login without signup
        Exception exception = assertThrows(Exception.class, () -> {
            UserService.login(email, password);
        });

        assertEquals("Invalid email or password", exception.getMessage());

        // Signup then try wrong password
        UserService.signup(email, fullName, password);

        exception = assertThrows(Exception.class, () -> {
            UserService.login(email, "wrongpass");
        });

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void testSignupValidation() {
        // Empty email
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            UserService.signup("", "Full Name", "password123");
        });
        assertEquals("Email cannot be empty", exception.getMessage());

        // Invalid email format
        exception = assertThrows(IllegalArgumentException.class, () -> {
            UserService.signup("invalid-email", "Full Name", "password123");
        });
        assertEquals("Invalid email format", exception.getMessage());

        // Empty full name
        exception = assertThrows(IllegalArgumentException.class, () -> {
            UserService.signup("test@example.com", "", "password123");
        });
        assertEquals("Full name cannot be empty", exception.getMessage());

        // Short password
        exception = assertThrows(IllegalArgumentException.class, () -> {
            UserService.signup("test@example.com", "Full Name", "123");
        });
        assertEquals("Password must be at least 6 characters long", exception.getMessage());
    }
}
