package com.donohoedigital.games.poker.service;

import com.donohoedigital.games.poker.service.impl.PasswordHashingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Tests for PasswordHashingService - bcrypt password hashing
 *
 * Tests written BEFORE implementation following RED-GREEN-REFACTOR cycle
 */
class PasswordHashingServiceTest {

    private PasswordHashingService service;

    @BeforeEach
    void setUp() {
        service = new PasswordHashingServiceImpl();
    }

    @Test
    void hashPassword_ShouldReturnBcryptHash_NotPlaintext() {
        // Given: a plaintext password
        String plaintext = "mySecretPassword123";

        // When: hashing the password
        String hash = service.hashPassword(plaintext);

        // Then: hash should start with $2a$ (bcrypt format) and not equal plaintext
        assertNotNull(hash, "Hash should not be null");
        assertTrue(hash.startsWith("$2a$"), "Hash should start with $2a$ (bcrypt format)");
        assertNotEquals(plaintext, hash, "Hash should not equal plaintext");
    }

    @Test
    void checkPassword_ShouldReturnTrue_WhenPasswordIsCorrect() {
        // Given: a password and its hash
        String plaintext = "correctPassword";
        String hash = service.hashPassword(plaintext);

        // When: checking the correct password
        boolean result = service.checkPassword(plaintext, hash);

        // Then: should return true
        assertTrue(result, "checkPassword should return true for correct password");
    }

    @Test
    void checkPassword_ShouldReturnFalse_WhenPasswordIsWrong() {
        // Given: a password and its hash
        String correctPassword = "correctPassword";
        String hash = service.hashPassword(correctPassword);

        // When: checking a wrong password
        String wrongPassword = "wrongPassword";
        boolean result = service.checkPassword(wrongPassword, hash);

        // Then: should return false
        assertFalse(result, "checkPassword should return false for wrong password");
    }

    @Test
    void hashPassword_ShouldProduceDifferentHashes_ForSamePassword() {
        // Given: the same password hashed twice
        String plaintext = "samePassword";
        String hash1 = service.hashPassword(plaintext);
        String hash2 = service.hashPassword(plaintext);

        // Then: hashes should be different (different salts)
        assertNotEquals(hash1, hash2,
                "Same password hashed twice should produce different hashes due to different salts");

        // But both should validate correctly
        assertTrue(service.checkPassword(plaintext, hash1), "First hash should validate");
        assertTrue(service.checkPassword(plaintext, hash2), "Second hash should validate");
    }

    @Test
    void checkPassword_ShouldReturnFalse_WhenPlaintextIsNull() {
        // Given: a valid hash
        String hash = service.hashPassword("password");

        // When: checking with null plaintext
        boolean result = service.checkPassword(null, hash);

        // Then: should return false
        assertFalse(result, "checkPassword should return false for null plaintext");
    }

    @Test
    void checkPassword_ShouldReturnFalse_WhenHashIsNull() {
        // Given: a plaintext password

        // When: checking with null hash
        boolean result = service.checkPassword("password", null);

        // Then: should return false
        assertFalse(result, "checkPassword should return false for null hash");
    }

    @Test
    void checkPassword_ShouldReturnFalse_WhenBothAreNull() {
        // When: checking with both null
        boolean result = service.checkPassword(null, null);

        // Then: should return false
        assertFalse(result, "checkPassword should return false when both are null");
    }

    @Test
    void hashPassword_ShouldHandleEmptyString() {
        // Given: an empty password
        String plaintext = "";

        // When: hashing the empty password
        String hash = service.hashPassword(plaintext);

        // Then: should still produce a valid hash
        assertNotNull(hash, "Hash should not be null even for empty string");
        assertTrue(hash.startsWith("$2a$"), "Hash should be valid bcrypt format");
        assertTrue(service.checkPassword("", hash), "Empty password should validate");
    }
}
