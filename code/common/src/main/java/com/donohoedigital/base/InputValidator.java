/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.base;

import java.util.regex.Pattern;

/**
 * Utility class for validating user input to prevent injection attacks and malformed data.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Email: RFC 5322 subset, max 254 chars</li>
 *   <li>String length: trimmed, configurable min/max</li>
 *   <li>Integer bounds: configurable range</li>
 * </ul>
 */
public class InputValidator
{
    /**
     * Email validation pattern - simplified RFC 5322 subset.
     * Allows: letters, numbers, +, _, ., - before @
     * Requires: @ with domain (letters, numbers, ., -)
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]+$"
    );

    /**
     * Maximum email length per RFC 5321
     */
    private static final int MAX_EMAIL_LENGTH = 254;

    /**
     * Private constructor - utility class should not be instantiated
     */
    private InputValidator()
    {
        throw new AssertionError("InputValidator is a utility class and should not be instantiated");
    }

    /**
     * Validates email format and length.
     *
     * @param email the email address to validate
     * @return true if email is valid, false otherwise
     */
    public static boolean isValidEmail(String email)
    {
        if (email == null || email.trim().isEmpty())
        {
            return false;
        }

        String trimmedEmail = email.trim();

        // Check length first (faster than regex)
        if (trimmedEmail.length() > MAX_EMAIL_LENGTH)
        {
            return false;
        }

        // Check pattern
        return EMAIL_PATTERN.matcher(trimmedEmail).matches();
    }

    /**
     * Validates string length after trimming.
     *
     * @param value the string to validate
     * @param minLength minimum length (inclusive)
     * @param maxLength maximum length (inclusive)
     * @return true if string length is within bounds, false otherwise
     */
    public static boolean isValidLength(String value, int minLength, int maxLength)
    {
        if (value == null)
        {
            return false;
        }

        String trimmed = value.trim();
        int length = trimmed.length();

        return length >= minLength && length <= maxLength;
    }

    /**
     * Validates integer is within specified bounds.
     *
     * @param value the integer to validate
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return true if value is within bounds, false otherwise
     */
    public static boolean isValidInt(int value, int min, int max)
    {
        return value >= min && value <= max;
    }

    // ========================================
    // Convenience methods for common validations
    // ========================================

    /**
     * Validates profile name (1-50 characters).
     *
     * @param name the profile name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidProfileName(String name)
    {
        return isValidLength(name, 1, 50);
    }

    /**
     * Validates game name (1-100 characters).
     *
     * @param name the game name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidGameName(String name)
    {
        return isValidLength(name, 1, 100);
    }

    /**
     * Validates chat message (1-500 characters).
     *
     * @param message the chat message to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidChatMessage(String message)
    {
        return isValidLength(message, 1, 500);
    }
}
