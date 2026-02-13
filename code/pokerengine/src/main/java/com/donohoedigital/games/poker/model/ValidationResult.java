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

import java.util.*;

/**
 * Container for validation warnings from tournament profile validation.
 *
 * <p>
 * Holds warnings and associated messages to display to the user.
 */
public class ValidationResult {
    private final List<ValidationWarning> warnings;
    private final Map<ValidationWarning, String> messages;

    /**
     * Create an empty validation result.
     */
    public ValidationResult() {
        this.warnings = new ArrayList<>();
        this.messages = new HashMap<>();
    }

    /**
     * Add a validation warning with an associated message.
     *
     * @param warning
     *            The type of warning
     * @param message
     *            The message to display for this warning
     */
    public void addWarning(ValidationWarning warning, String message) {
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
        messages.put(warning, message);
    }

    /**
     * Check if any warnings were found.
     *
     * @return true if warnings exist
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Get all warnings found during validation.
     *
     * @return Unmodifiable list of warnings
     */
    public List<ValidationWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Get the message associated with a specific warning.
     *
     * @param warning
     *            The warning type
     * @return The message, or null if not found
     */
    public String getMessage(ValidationWarning warning) {
        return messages.get(warning);
    }

    /**
     * Get all warning messages.
     *
     * @return Unmodifiable map of warnings to messages
     */
    public Map<ValidationWarning, String> getMessages() {
        return Collections.unmodifiableMap(messages);
    }
}
