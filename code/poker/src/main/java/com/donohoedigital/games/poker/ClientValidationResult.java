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
package com.donohoedigital.games.poker;

import java.util.*;

/**
 * Container for validation warnings from tournament profile validation.
 *
 * <p>
 * Client-side version of {@code ValidationResult}.
 */
public class ClientValidationResult {
    private final List<ClientValidationWarning> warnings;
    private final Map<ClientValidationWarning, String> messages;

    public ClientValidationResult() {
        this.warnings = new ArrayList<>();
        this.messages = new HashMap<>();
    }

    public void addWarning(ClientValidationWarning warning, String message) {
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
        messages.put(warning, message);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<ClientValidationWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public String getMessage(ClientValidationWarning warning) {
        return messages.get(warning);
    }

    public Map<ClientValidationWarning, String> getMessages() {
        return Collections.unmodifiableMap(messages);
    }
}
