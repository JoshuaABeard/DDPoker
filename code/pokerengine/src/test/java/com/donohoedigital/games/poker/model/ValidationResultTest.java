/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for ValidationResult.
 */
public class ValidationResultTest {

    private ValidationResult result;

    @BeforeEach
    public void setUp() {
        result = new ValidationResult();
    }

    @Test
    public void testDefaultConstructor() {
        ValidationResult newResult = new ValidationResult();
        assertNotNull(newResult);
        assertFalse(newResult.hasWarnings());
        assertTrue(newResult.getWarnings().isEmpty());
    }

    @Test
    public void testHasWarningsWhenEmpty() {
        assertFalse(result.hasWarnings(), "Should have no warnings initially");
    }

    @Test
    public void testHasWarningsWhenNotEmpty() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Test message");
        assertTrue(result.hasWarnings(), "Should have warnings after adding");
    }

    @Test
    public void testAddWarning() {
        result.addWarning(ValidationWarning.SHALLOW_STARTING_DEPTH, "Stack too shallow");

        assertTrue(result.hasWarnings(), "Should have warnings");
        assertEquals(1, result.getWarnings().size(), "Should have 1 warning");
        assertTrue(result.getWarnings().contains(ValidationWarning.SHALLOW_STARTING_DEPTH),
                "Should contain the warning");
    }

    @Test
    public void testAddMultipleWarnings() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Message 1");
        result.addWarning(ValidationWarning.TOO_MANY_PAYOUT_SPOTS, "Message 2");
        result.addWarning(ValidationWarning.EXCESSIVE_HOUSE_TAKE, "Message 3");

        assertEquals(3, result.getWarnings().size(), "Should have 3 warnings");
    }

    @Test
    public void testAddDuplicateWarning() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "First message");
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Second message");

        assertEquals(1, result.getWarnings().size(), "Should only have 1 warning (no duplicates)");
        assertEquals("Second message", result.getMessage(ValidationWarning.UNREACHABLE_LEVELS),
                "Should use latest message");
    }

    @Test
    public void testGetWarnings() {
        result.addWarning(ValidationWarning.SHALLOW_STARTING_DEPTH, "Message 1");
        result.addWarning(ValidationWarning.EXCESSIVE_HOUSE_TAKE, "Message 2");

        List<ValidationWarning> warnings = result.getWarnings();

        assertNotNull(warnings, "Warnings list should not be null");
        assertEquals(2, warnings.size(), "Should have 2 warnings");
        assertTrue(warnings.contains(ValidationWarning.SHALLOW_STARTING_DEPTH),
                "Should contain SHALLOW_STARTING_DEPTH");
        assertTrue(warnings.contains(ValidationWarning.EXCESSIVE_HOUSE_TAKE), "Should contain EXCESSIVE_HOUSE_TAKE");
    }

    @Test
    public void testGetWarningsReturnsUnmodifiableList() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Message");

        List<ValidationWarning> warnings = result.getWarnings();

        try {
            warnings.add(ValidationWarning.TOO_MANY_PAYOUT_SPOTS);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetMessage() {
        result.addWarning(ValidationWarning.TOO_MANY_PAYOUT_SPOTS, "Too many payout spots configured");

        String message = result.getMessage(ValidationWarning.TOO_MANY_PAYOUT_SPOTS);

        assertNotNull(message, "Message should not be null");
        assertEquals("Too many payout spots configured", message, "Should return correct message");
    }

    @Test
    public void testGetMessageForNonExistentWarning() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Some message");

        String message = result.getMessage(ValidationWarning.EXCESSIVE_HOUSE_TAKE);

        assertNull(message, "Should return null for non-existent warning");
    }

    @Test
    public void testGetMessages() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Message 1");
        result.addWarning(ValidationWarning.SHALLOW_STARTING_DEPTH, "Message 2");

        Map<ValidationWarning, String> messages = result.getMessages();

        assertNotNull(messages, "Messages map should not be null");
        assertEquals(2, messages.size(), "Should have 2 messages");
        assertEquals("Message 1", messages.get(ValidationWarning.UNREACHABLE_LEVELS),
                "Should have correct message for UNREACHABLE_LEVELS");
        assertEquals("Message 2", messages.get(ValidationWarning.SHALLOW_STARTING_DEPTH),
                "Should have correct message for SHALLOW_STARTING_DEPTH");
    }

    @Test
    public void testGetMessagesReturnsUnmodifiableMap() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Message");

        Map<ValidationWarning, String> messages = result.getMessages();

        try {
            messages.put(ValidationWarning.TOO_MANY_PAYOUT_SPOTS, "New message");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetMessagesWhenEmpty() {
        Map<ValidationWarning, String> messages = result.getMessages();

        assertNotNull(messages, "Messages map should not be null");
        assertTrue(messages.isEmpty(), "Messages map should be empty");
    }
}
