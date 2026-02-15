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

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for ValidationResult.
 */
public class ValidationResultTest {

    private ValidationResult result;

    @Before
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
        assertFalse("Should have no warnings initially", result.hasWarnings());
    }

    @Test
    public void testHasWarningsWhenNotEmpty() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Test message");
        assertTrue("Should have warnings after adding", result.hasWarnings());
    }

    @Test
    public void testAddWarning() {
        result.addWarning(ValidationWarning.SHALLOW_STARTING_DEPTH, "Stack too shallow");

        assertTrue("Should have warnings", result.hasWarnings());
        assertEquals("Should have 1 warning", 1, result.getWarnings().size());
        assertTrue("Should contain the warning",
                result.getWarnings().contains(ValidationWarning.SHALLOW_STARTING_DEPTH));
    }

    @Test
    public void testAddMultipleWarnings() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Message 1");
        result.addWarning(ValidationWarning.TOO_MANY_PAYOUT_SPOTS, "Message 2");
        result.addWarning(ValidationWarning.EXCESSIVE_HOUSE_TAKE, "Message 3");

        assertEquals("Should have 3 warnings", 3, result.getWarnings().size());
    }

    @Test
    public void testAddDuplicateWarning() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "First message");
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Second message");

        assertEquals("Should only have 1 warning (no duplicates)", 1, result.getWarnings().size());
        assertEquals("Should use latest message", "Second message",
                result.getMessage(ValidationWarning.UNREACHABLE_LEVELS));
    }

    @Test
    public void testGetWarnings() {
        result.addWarning(ValidationWarning.SHALLOW_STARTING_DEPTH, "Message 1");
        result.addWarning(ValidationWarning.EXCESSIVE_HOUSE_TAKE, "Message 2");

        List<ValidationWarning> warnings = result.getWarnings();

        assertNotNull("Warnings list should not be null", warnings);
        assertEquals("Should have 2 warnings", 2, warnings.size());
        assertTrue("Should contain SHALLOW_STARTING_DEPTH",
                warnings.contains(ValidationWarning.SHALLOW_STARTING_DEPTH));
        assertTrue("Should contain EXCESSIVE_HOUSE_TAKE", warnings.contains(ValidationWarning.EXCESSIVE_HOUSE_TAKE));
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

        assertNotNull("Message should not be null", message);
        assertEquals("Should return correct message", "Too many payout spots configured", message);
    }

    @Test
    public void testGetMessageForNonExistentWarning() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Some message");

        String message = result.getMessage(ValidationWarning.EXCESSIVE_HOUSE_TAKE);

        assertNull("Should return null for non-existent warning", message);
    }

    @Test
    public void testGetMessages() {
        result.addWarning(ValidationWarning.UNREACHABLE_LEVELS, "Message 1");
        result.addWarning(ValidationWarning.SHALLOW_STARTING_DEPTH, "Message 2");

        Map<ValidationWarning, String> messages = result.getMessages();

        assertNotNull("Messages map should not be null", messages);
        assertEquals("Should have 2 messages", 2, messages.size());
        assertEquals("Should have correct message for UNREACHABLE_LEVELS", "Message 1",
                messages.get(ValidationWarning.UNREACHABLE_LEVELS));
        assertEquals("Should have correct message for SHALLOW_STARTING_DEPTH", "Message 2",
                messages.get(ValidationWarning.SHALLOW_STARTING_DEPTH));
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

        assertNotNull("Messages map should not be null", messages);
        assertTrue("Messages map should be empty", messages.isEmpty());
    }
}
