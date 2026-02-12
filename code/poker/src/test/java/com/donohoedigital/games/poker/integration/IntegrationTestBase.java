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
package com.donohoedigital.games.poker.integration;

import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.config.ConfigTestHelper;
import com.donohoedigital.games.poker.mock.MockGameEngine;
import com.donohoedigital.games.poker.mock.MockPokerMain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;

/**
 * Base class for integration tests requiring GameEngine/PokerMain
 * infrastructure.
 *
 * <p>
 * This class provides:
 * </p>
 * <ul>
 * <li>Mock GameEngine initialization (for GameEngine.getGameEngine())</li>
 * <li>Mock PokerMain initialization (for PokerMain.getPokerMain())</li>
 * <li>ConfigManager setup for headless testing</li>
 * <li>Automatic cleanup after all tests complete</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * &#64;Tag("integration")
 * class MyIntegrationTest extends IntegrationTestBase {
 *
 * 	&#64;Test
 * 	void should_DoSomething_When_ConditionMet() {
 * 		// GameEngine.getGameEngine() and PokerMain.getPokerMain() now work
 * 		// ... test code ...
 * 	}
 * }
 * </pre>
 *
 * <h2>Test Instance Lifecycle</h2>
 * <p>
 * This class uses {@code TestInstance.Lifecycle.PER_CLASS} to ensure
 * setup/teardown runs once for all tests in the class, not per test. This is
 * more efficient for integration tests that need expensive infrastructure
 * setup.
 * </p>
 *
 * <h2>Running Integration Tests</h2>
 * <p>
 * Integration tests are tagged with {@code @Tag("integration")} and can be run
 * separately:
 * </p>
 *
 * <pre>
 * # Run only unit tests (default)
 * mvn test
 *
 * # Run only integration tests
 * mvn test -Dgroups=integration
 *
 * # Run all tests
 * mvn test -Dgroups="unit,integration"
 * </pre>
 *
 * <h2>Limitations</h2>
 * <p>
 * The mock infrastructure provides minimal functionality:
 * </p>
 * <ul>
 * <li>No actual GUI components or windows</li>
 * <li>No actual event processing</li>
 * <li>No phase management</li>
 * <li>Limited GameEngine methods stubbed</li>
 * </ul>
 *
 * <p>
 * Tests should focus on business logic, not UI or event system behavior.
 * </p>
 */
@Tag("slow")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    private static final Logger logger = LogManager.getLogger(IntegrationTestBase.class);

    /**
     * Set up test infrastructure before all tests in the class.
     *
     * <p>
     * Initializes:
     * </p>
     * <ul>
     * <li>ConfigManager for headless testing</li>
     * <li>MockGameEngine singleton</li>
     * <li>MockPokerMain singleton</li>
     * </ul>
     *
     * @throws RuntimeException
     *             if infrastructure initialization fails
     */
    @BeforeAll
    void setupIntegrationInfrastructure() {
        logger.info("Setting up integration test infrastructure");

        try {
            // Initialize ConfigManager with GUI config using helper
            ConfigManager configMgr = ConfigTestHelper.initializeWithGuiForTesting("poker");
            logger.debug("ConfigManager initialized for headless testing with GUI config");

            // Initialize mock GameEngine (must be first - PokerMain extends GameEngine)
            MockGameEngine.initializeForTesting();
            logger.debug("MockGameEngine initialized");

            // Initialize mock PokerMain
            MockPokerMain.initializeForTesting();
            logger.debug("MockPokerMain initialized");

            logger.info("Integration test infrastructure ready");

        } catch (Exception e) {
            logger.error("Failed to set up integration test infrastructure", e);
            throw new RuntimeException("Integration test infrastructure setup failed", e);
        }
    }

    /**
     * Tear down test infrastructure after all tests in the class.
     *
     * <p>
     * Cleans up:
     * </p>
     * <ul>
     * <li>MockPokerMain singleton</li>
     * <li>MockGameEngine singleton</li>
     * <li>ConfigManager and all config singletons</li>
     * </ul>
     *
     * <p>
     * This prevents test pollution between test classes and enables parallel test
     * execution.
     * </p>
     */
    @AfterAll
    void teardownIntegrationInfrastructure() {
        logger.info("Tearing down integration test infrastructure");

        try {
            // Reset mocks in reverse order of initialization
            MockPokerMain.resetForTesting();
            logger.debug("MockPokerMain reset");

            MockGameEngine.resetForTesting();
            logger.debug("MockGameEngine reset");

            // Reset all config singletons to allow parallel test execution
            ConfigTestHelper.resetForTesting();
            logger.debug("ConfigManager and all config singletons reset");

            logger.info("Integration test infrastructure cleaned up");

        } catch (Exception e) {
            logger.warn("Error during integration test infrastructure teardown", e);
            // Don't throw - let tests complete even if cleanup has issues
        }
    }
}
