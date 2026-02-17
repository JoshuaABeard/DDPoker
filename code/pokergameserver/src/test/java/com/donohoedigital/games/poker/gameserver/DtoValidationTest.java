/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.gameserver.dto.LoginRequest;
import com.donohoedigital.games.poker.gameserver.dto.RegisterRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Tests that Bean Validation constraints are correctly applied to request DTOs.
 */
class DtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ====================================
    // RegisterRequest
    // ====================================

    @Test
    void registerRequest_validRequest_noViolations() {
        RegisterRequest req = new RegisterRequest("alice_01", "password123", "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void registerRequest_blankUsername_fails() {
        RegisterRequest req = new RegisterRequest("", "password123", "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Blank username should fail validation");
    }

    @Test
    void registerRequest_usernameTooShort_fails() {
        RegisterRequest req = new RegisterRequest("ab", "password123", "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Username shorter than 3 chars should fail");
    }

    @Test
    void registerRequest_usernameTooLong_fails() {
        String longName = "a".repeat(51);
        RegisterRequest req = new RegisterRequest(longName, "password123", "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Username longer than 50 chars should fail");
    }

    @Test
    void registerRequest_usernameWithInvalidChars_fails() {
        RegisterRequest req = new RegisterRequest("alice bob!", "password123", "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Username with spaces/special chars should fail");
    }

    @Test
    void registerRequest_usernameValidChars_passes() {
        RegisterRequest req = new RegisterRequest("alice-bob_01", "password123", "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Username with letters, numbers, underscores, hyphens should pass");
    }

    @Test
    void registerRequest_passwordTooShort_fails() {
        RegisterRequest req = new RegisterRequest("alice01", "short", "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Password shorter than 8 chars should fail");
    }

    @Test
    void registerRequest_passwordTooLong_fails() {
        String longPass = "a".repeat(129);
        RegisterRequest req = new RegisterRequest("alice01", longPass, "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Password longer than 128 chars should fail");
    }

    @Test
    void registerRequest_blankPassword_fails() {
        RegisterRequest req = new RegisterRequest("alice01", "", "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Blank password should fail");
    }

    @Test
    void registerRequest_invalidEmail_fails() {
        RegisterRequest req = new RegisterRequest("alice01", "password123", "not-an-email");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Invalid email should fail");
    }

    @Test
    void registerRequest_blankEmail_fails() {
        RegisterRequest req = new RegisterRequest("alice01", "password123", "");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Blank email should fail");
    }

    @Test
    void registerRequest_nullUsername_fails() {
        RegisterRequest req = new RegisterRequest(null, "password123", "alice@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Null username should fail");
    }

    // ====================================
    // LoginRequest
    // ====================================

    @Test
    void loginRequest_validRequest_noViolations() {
        LoginRequest req = new LoginRequest("alice", "password123", false);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Valid login request should have no violations");
    }

    @Test
    void loginRequest_blankUsername_fails() {
        LoginRequest req = new LoginRequest("", "password123", false);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Blank username should fail");
    }

    @Test
    void loginRequest_nullUsername_fails() {
        LoginRequest req = new LoginRequest(null, "password123", false);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Null username should fail");
    }

    @Test
    void loginRequest_blankPassword_fails() {
        LoginRequest req = new LoginRequest("alice", "", false);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Blank password should fail");
    }

    @Test
    void loginRequest_nullPassword_fails() {
        LoginRequest req = new LoginRequest("alice", null, false);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Null password should fail");
    }

    @Test
    void loginRequest_rememberMe_true_passes() {
        LoginRequest req = new LoginRequest("alice", "password123", true);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Valid request with rememberMe=true should pass");
    }
}
