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
package com.donohoedigital.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class for managing ConfigManager and related singletons in tests.
 * <p>
 * This class provides methods to safely initialize and reset configuration
 * singletons between test classes, enabling parallel test execution without
 * singleton conflicts.
 * </p>
 *
 * <h2>Usage in Tests</h2>
 *
 * <pre>
 * &#64;BeforeAll
 * static void setupConfig() {
 * 	ConfigTestHelper.initializeForTesting("poker", ApplicationType.HEADLESS_CLIENT);
 * }
 *
 * &#64;AfterAll
 * static void cleanupConfig() {
 * 	ConfigTestHelper.resetForTesting();
 * }
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This helper ensures that configuration singletons are properly reset between
 * test classes, allowing tests to run in parallel at the class level without
 * interference.
 * </p>
 *
 * <h2>What Gets Reset</h2>
 * <ul>
 * <li>ConfigManager</li>
 * <li>PropertyConfig</li>
 * <li>DataElementConfig</li>
 * <li>AudioConfig</li>
 * <li>HelpConfig</li>
 * <li>ImageConfig</li>
 * <li>StylesConfig</li>
 * </ul>
 */
public class ConfigTestHelper {

    private static final Logger logger = LogManager.getLogger(ConfigTestHelper.class);

    /**
     * Initialize ConfigManager for testing with headless client type.
     * <p>
     * This is a convenience method for the most common test scenario.
     * </p>
     *
     * @param appName
     *            Application name (e.g., "poker")
     * @return The initialized ConfigManager instance
     */
    public static ConfigManager initializeForTesting(String appName) {
        return initializeForTesting(appName, ApplicationType.HEADLESS_CLIENT);
    }

    /**
     * Initialize ConfigManager for testing with specified application type.
     * <p>
     * If ConfigManager is already initialized, this method will reset it first to
     * ensure clean state.
     * </p>
     *
     * @param appName
     *            Application name (e.g., "poker")
     * @param type
     *            Application type
     * @return The initialized ConfigManager instance
     */
    public static ConfigManager initializeForTesting(String appName, ApplicationType type) {
        return initializeForTesting(appName, type, true);
    }

    /**
     * Initialize ConfigManager for testing with full control over parameters.
     *
     * @param appName
     *            Application name (e.g., "poker")
     * @param type
     *            Application type
     * @param allowOverrides
     *            Whether to allow property overrides
     * @return The initialized ConfigManager instance
     */
    public static ConfigManager initializeForTesting(String appName, ApplicationType type, boolean allowOverrides) {
        // Reset if already initialized
        if (ConfigManager.getConfigManager() != null) {
            logger.debug("ConfigManager already initialized, resetting before re-initialization");
            resetForTesting();
        }

        logger.debug("Initializing ConfigManager for testing: appName={}, type={}", appName, type);
        return new ConfigManager(appName, type, allowOverrides);
    }

    /**
     * Initialize ConfigManager for testing and load GUI config.
     * <p>
     * This is useful for tests that need StylesConfig or ImageConfig, which are
     * normally only loaded for CLIENT type.
     * </p>
     *
     * @param appName
     *            Application name (e.g., "poker")
     * @return The initialized ConfigManager instance
     */
    public static ConfigManager initializeWithGuiForTesting(String appName) {
        ConfigManager configMgr = initializeForTesting(appName, ApplicationType.HEADLESS_CLIENT);
        logger.debug("Loading GUI config for testing");
        configMgr.loadGuiConfig();
        return configMgr;
    }

    /**
     * Reset all configuration singletons.
     * <p>
     * This method should be called in &#64;AfterAll to ensure clean state for the
     * next test class when running tests in parallel.
     * </p>
     * <p>
     * <strong>IMPORTANT:</strong> ConfigManager.resetForTesting() calls reset on
     * all child config singletons, so we only need to call it once.
     * </p>
     */
    public static void resetForTesting() {
        logger.debug("Resetting all config singletons for testing");

        // ConfigManager.resetForTesting() resets all child singletons too
        ConfigManager.resetForTesting();

        logger.debug("Config singletons reset complete");
    }

    /**
     * Check if ConfigManager is initialized.
     *
     * @return true if ConfigManager is initialized, false otherwise
     */
    public static boolean isInitialized() {
        return ConfigManager.getConfigManager() != null;
    }

    /**
     * Suppress warnings about ConfigManager already being initialized.
     * <p>
     * This is useful in scenarios where you want to explicitly reinitialize without
     * seeing warning logs.
     * </p>
     * <p>
     * <strong>Note:</strong> Currently ConfigManager shows warnings but doesn't
     * prevent reinitialization. This method is a placeholder for future
     * enhancement.
     * </p>
     */
    @Deprecated
    public static void suppressReinitializationWarnings() {
        // Placeholder for future enhancement
        // Currently ConfigManager uses ApplicationError.warnNotNull which logs but
        // doesn't prevent
    }
}
