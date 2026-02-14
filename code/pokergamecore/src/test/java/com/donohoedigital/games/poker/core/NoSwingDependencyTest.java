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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.core;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Verification test to ensure pokergamecore has NO Swing or AWT dependencies.
 * This module must remain pure game logic - no UI code.
 */
class NoSwingDependencyTest {

    private static final String[] FORBIDDEN_IMPORTS = {"javax.swing.", "java.awt.event.", "java.awt.Component",
            "java.awt.Container", "java.awt.Dimension", "java.awt.Font", "java.awt.Frame", "java.awt.Graphics",
            "java.awt.Image", "java.awt.Toolkit", "java.awt.Window"
            // Note: java.awt.Color is allowed (used in poker domain model)
    };

    @Test
    void pokergamecore_shouldNotImportSwingOrAWT() throws IOException {
        // Find pokergamecore source root
        Path projectRoot = findProjectRoot();
        Path sourceRoot = projectRoot.resolve("src/main/java");

        assertThat(sourceRoot).exists();

        // Scan all .java files
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(javaFile -> {
                try {
                    List<String> lines = Files.readAllLines(javaFile);
                    checkForForbiddenImports(javaFile, lines, violations);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read file: " + javaFile, e);
                }
            });
        }

        // Report violations
        if (!violations.isEmpty()) {
            fail("Found forbidden Swing/AWT imports in pokergamecore:\n" + String.join("\n", violations));
        }
    }

    private void checkForForbiddenImports(Path file, List<String> lines, List<String> violations) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            // Check if it's an import statement
            if (line.startsWith("import ")) {
                for (String forbidden : FORBIDDEN_IMPORTS) {
                    if (line.contains(forbidden)) {
                        violations.add(String.format("  %s:%d - %s", file.getFileName(), i + 1, line));
                    }
                }
            }
        }
    }

    private Path findProjectRoot() {
        // Start from current directory and walk up to find pokergamecore module
        Path current = Path.of("").toAbsolutePath();

        while (current != null) {
            Path pomFile = current.resolve("pom.xml");
            if (Files.exists(pomFile)) {
                try {
                    String content = Files.readString(pomFile);
                    if (content.contains("<artifactId>pokergamecore</artifactId>")) {
                        return current;
                    }
                } catch (IOException e) {
                    // Ignore and continue
                }
            }
            current = current.getParent();
        }

        throw new IllegalStateException("Could not find pokergamecore module root");
    }
}
