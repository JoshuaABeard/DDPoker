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
package com.donohoedigital.games.poker.mock;

import com.donohoedigital.games.engine.GameEngine;

import java.lang.reflect.Field;

/**
 * Minimal GameEngine mock for integration tests.
 * Provides just enough functionality to test poker game logic without requiring
 * full GUI infrastructure.
 *
 * <p>This mock allows integration tests to:</p>
 * <ul>
 *   <li>Call GameEngine.getGameEngine() without NPE</li>
 *   <li>Run tests in headless mode without GUI components</li>
 * </ul>
 *
 * <p><strong>Important:</strong> This is a minimal mock. It does NOT provide:</p>
 * <ul>
 *   <li>Actual GUI components or windows</li>
 *   <li>Event processing or phase management</li>
 *   <li>Full game engine initialization</li>
 * </ul>
 *
 * <p>Use {@link IntegrationTestBase} to automatically set up and tear down this mock.</p>
 */
public class MockGameEngine {

    private static GameEngine mockInstance;

    /**
     * Initialize mock GameEngine for testing.
     * Sets the GameEngine singleton to a minimal mock instance.
     *
     * @throws RuntimeException if reflection fails to set singleton
     */
    public static void initializeForTesting() {
        if (mockInstance != null) {
            return; // Already initialized
        }

        try {
            // Create a minimal GameEngine subclass
            mockInstance = new GameEngine("poker", "poker", "test", new String[0], true) {
                @Override
                public com.donohoedigital.comms.Version getVersion() {
                    return new com.donohoedigital.comms.Version("1.0.0-test");
                }
            };

            // Use reflection to set the private static engine_ field in GameEngine
            Field engineField = GameEngine.class.getDeclaredField("engine_");
            engineField.setAccessible(true);
            engineField.set(null, mockInstance);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MockGameEngine", e);
        }
    }

    /**
     * Reset mock GameEngine after testing.
     * Clears the GameEngine singleton to prevent test pollution.
     *
     * @throws RuntimeException if reflection fails to clear singleton
     */
    public static void resetForTesting() {
        try {
            Field engineField = GameEngine.class.getDeclaredField("engine_");
            engineField.setAccessible(true);
            engineField.set(null, null);
            mockInstance = null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset MockGameEngine", e);
        }
    }

    /**
     * Get the mock GameEngine instance.
     * Useful for verifying the mock is initialized correctly.
     *
     * @return the mock GameEngine instance, or null if not initialized
     */
    public static GameEngine getMockInstance() {
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
}
