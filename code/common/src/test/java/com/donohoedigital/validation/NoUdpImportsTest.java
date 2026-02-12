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
package com.donohoedigital.validation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Verifies that UDP module imports have been completely removed from the
 * codebase. Part of Phase 3 of UDP-to-TCP conversion.
 */
class NoUdpImportsTest {

    private static final String UDP_IMPORT_PATTERN = "import com.donohoedigital.udp";

    @Test
    void testNoUdpImportsInPokerModule() throws IOException {
        List<Path> violations = scanForUdpImports("poker");
        assertThat(violations).withFailMessage("Found UDP imports in poker module: %s", violations).isEmpty();
    }

    @Test
    void testNoUdpImportsInPokerNetwork() throws IOException {
        List<Path> violations = scanForUdpImports("pokernetwork");
        assertThat(violations).withFailMessage("Found UDP imports in pokernetwork module: %s", violations).isEmpty();
    }

    @Test
    void testNoUdpImportsInPokerServer() throws IOException {
        List<Path> violations = scanForUdpImports("pokerserver");
        assertThat(violations).withFailMessage("Found UDP imports in pokerserver module: %s", violations).isEmpty();
    }

    @Test
    void testNoUdpImportsInGameEngine() throws IOException {
        List<Path> violations = scanForUdpImports("gameengine");
        assertThat(violations).withFailMessage("Found UDP imports in gameengine module: %s", violations).isEmpty();
    }

    /**
     * Scans the given module for UDP imports.
     *
     * @param moduleName
     *            the module to scan
     * @return list of files containing UDP imports
     */
    private List<Path> scanForUdpImports(String moduleName) throws IOException {
        List<Path> violations = new ArrayList<>();

        // Get the module directory relative to the common module
        Path moduleDir = Paths.get("..", moduleName, "src", "main", "java");

        if (!Files.exists(moduleDir)) {
            // Module doesn't exist or path is wrong - skip
            return violations;
        }

        try (Stream<Path> paths = Files.walk(moduleDir)) {
            paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    String content = Files.readString(path);
                    if (content.contains(UDP_IMPORT_PATTERN)) {
                        violations.add(path);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read file: " + path, e);
                }
            });
        }

        return violations;
    }
}
