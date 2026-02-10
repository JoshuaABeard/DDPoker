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
package com.donohoedigital.games.poker.mock;

import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.PokerMain;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal PokerMain mock for integration tests.
 * Provides computer player names without requiring full application initialization.
 *
 * <p>This mock allows integration tests to:</p>
 * <ul>
 *   <li>Call PokerMain.getPokerMain() without NPE</li>
 *   <li>Get AI player names for tournament setup</li>
 *   <li>Initialize tournaments with computer players</li>
 * </ul>
 *
 * <p><strong>Important:</strong> This mock extends GameEngine via PokerMain.
 * You should initialize {@link MockGameEngine} BEFORE initializing this mock.</p>
 *
 * <p>Use {@link IntegrationTestBase} to automatically set up and tear down mocks.</p>
 */
public class MockPokerMain {

    private static PokerMain mockInstance;

    // Default AI player names for testing
    private static final List<String> AI_NAMES = List.of(
            "Computer Alice",
            "Computer Bob",
            "Computer Charlie",
            "Computer Diana",
            "Computer Eve",
            "Computer Frank",
            "Computer Grace",
            "Computer Henry",
            "Computer Ivy",
            "Computer Jack"
    );

    /**
     * Initialize mock PokerMain for testing.
     * Sets the PokerMain singleton (via GameEngine) to a minimal mock instance.
     *
     * <p><strong>Prerequisite:</strong> {@link MockGameEngine} must be initialized first.</p>
     *
     * @throws RuntimeException if MockGameEngine not initialized or reflection fails
     */
    public static void initializeForTesting() {
        if (mockInstance != null) {
            return; // Already initialized
        }

        if (!MockGameEngine.isInitialized()) {
            throw new IllegalStateException(
                    "MockGameEngine must be initialized before MockPokerMain. " +
                    "Call MockGameEngine.initializeForTesting() first."
            );
        }

        try {
            // Create a minimal PokerMain subclass
            // Constructor signature: PokerMain(configName, mainModule, args, headless, loadNames)
            mockInstance = new PokerMain("poker", "poker", new String[0], true, false) {
                @Override
                public com.donohoedigital.comms.Version getVersion() {
                    return new com.donohoedigital.comms.Version("1.0.0-test");
                }

                @Override
                public List<String> getNames() {
                    return new ArrayList<>(AI_NAMES);
                }

                @Override
                public boolean isDemo() {
                    return false; // Tests run in full mode
                }
            };

            // Use reflection to set the private static engine_ field in GameEngine
            // This makes getGameEngine() return our PokerMain instance
            Field engineField = GameEngine.class.getDeclaredField("engine_");
            engineField.setAccessible(true);
            engineField.set(null, mockInstance);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MockPokerMain", e);
        }
    }

    /**
     * Reset mock PokerMain after testing.
     * Clears the singleton to prevent test pollution.
     *
     * <p><strong>Note:</strong> This also clears the GameEngine singleton.</p>
     *
     * @throws RuntimeException if reflection fails
     */
    public static void resetForTesting() {
        try {
            Field engineField = GameEngine.class.getDeclaredField("engine_");
            engineField.setAccessible(true);
            engineField.set(null, null);
            mockInstance = null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset MockPokerMain", e);
        }
    }

    /**
     * Get the mock PokerMain instance.
     * Useful for verifying the mock is initialized correctly.
     *
     * @return the mock PokerMain instance, or null if not initialized
     */
    public static PokerMain getMockInstance() {
        return mockInstance;
    }

    /**
     * Check if mock is initialized.
     *
     * @return true if mock is initialized, false otherwise
     */
    public static boolean isInitialized() {
        return mockInstance != null;
    }

    /**
     * Get the AI names used by the mock.
     * Useful for test assertions.
     *
     * @return list of AI player names
     */
    public static List<String> getAiNames() {
        return new ArrayList<>(AI_NAMES);
    }
}
