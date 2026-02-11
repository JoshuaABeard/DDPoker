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
package com.donohoedigital.games.poker.ui;

import com.donohoedigital.config.ConfigTestHelper;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.ui.matchers.PokerMatchers;
import com.donohoedigital.gui.InternalDialog;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.testing.AssertJSwingTestCaseTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;

/**
 * Base class for UI tests using AssertJ Swing.
 * Provides common setup/teardown and utilities for testing the DD Poker Swing application.
 * Adapted for JUnit 5.
 *
 * IMPORTANT: UI tests are automatically disabled in headless environments.
 * They will only run on systems with a graphical display available.
 */
@EnabledIfDisplay
public abstract class PokerUITestBase extends AssertJSwingTestCaseTemplate
{
    protected FrameFixture window;
    protected PokerMain pokerMain;

    @BeforeAll
    public static void setUpOnce()
    {
        // Configure headless mode for CI/CD environments
        // This allows GUI tests to run without a display
        System.setProperty("java.awt.headless", "false"); // Must be false for Swing components

        // However, we can configure the robot to not require actual display interaction
        FailOnThreadViolationRepaintManager.install();
    }

    /**
     * Set up the robot and launch the application.
     */
    @BeforeEach
    protected final void setUp()
    {
        setUpRobot();

        // Initialize config for poker with GUI support
        ConfigTestHelper.initializeWithGuiForTesting("poker");

        // Launch the application on the EDT
        System.out.println("Launching PokerMain...");
        SwingUtilities.invokeLater(() -> {
            try {
                // Launch without arguments - PokerMain doesn't have a -test flag
                pokerMain = new PokerMain("poker", "poker", new String[]{});
                pokerMain.init();
                System.out.println("PokerMain initialized");
            } catch (Exception e) {
                System.err.println("Failed to start PokerMain: " + e.getMessage());
                throw new RuntimeException("Failed to start PokerMain", e);
            }
        });

        // Wait for EDT to complete
        robot().waitForIdle();
        System.out.println("Waiting for main frame...");

        // NOW that GameEngine is initialized, we can set up profiles
        // This prevents FirstTimeWizard from blocking (for non-wizard tests)
        try {
            TestProfileHelper.setupForNonWizardTests("UITestDefaultProfile");
            System.out.println("Profile setup complete");
        } catch (Exception e) {
            System.err.println("Profile setup failed: " + e.getMessage());
            // Continue anyway - some tests might not need profiles
        }

        // Wait for and capture the main frame with timeout
        try {
            JFrame mainFrame = findMainFrameWithTimeout(robot(), 10000);
            window = new FrameFixture(robot(), mainFrame);
            window.show(); // Brings frame to front
            System.out.println("Main frame found and shown");
        } catch (Exception e) {
            System.err.println("Failed to find main frame: " + e.getMessage());
            throw new RuntimeException("Failed to find main frame", e);
        }
    }

    protected void onTearDown()
    {
        if (window != null) {
            window.cleanUp();
        }
        if (pokerMain != null) {
            // Cleanup poker main if needed
            pokerMain = null;
        }
        // Reset config singletons for next test
        ConfigTestHelper.resetForTesting();
    }

    /**
     * Find the main DD Poker frame with timeout.
     * Uses a matcher to find the frame by title or type.
     */
    private JFrame findMainFrameWithTimeout(Robot robot, long timeoutMs)
    {
        long startTime = System.currentTimeMillis();
        Exception lastException = null;

        while (System.currentTimeMillis() - startTime < timeoutMs)
        {
            try {
                return robot.finder().find(new GenericTypeMatcher<JFrame>(JFrame.class) {
                    @Override
                    protected boolean isMatching(JFrame frame) {
                        String title = frame.getTitle();
                        boolean matches = title != null && (title.contains("DD Poker") || title.contains("poker"));
                        if (matches) {
                            System.out.println("Found frame with title: " + title);
                        }
                        return matches;
                    }
                });
            } catch (Exception e) {
                lastException = e;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for frame", ie);
                }
            }
        }

        throw new RuntimeException("Timeout waiting for main frame", lastException);
    }

    /**
     * Utility method to take a screenshot (useful for debugging).
     * Screenshots are saved to target/screenshots/
     */
    protected void takeScreenshot(String name)
    {
        try {
            java.io.File dir = new java.io.File("target/screenshots");
            dir.mkdirs();

            Component component = window.target();
            Rectangle bounds = component.getBounds();
            java.awt.image.BufferedImage screenshot = new java.awt.Robot()
                .createScreenCapture(new Rectangle(
                    component.getLocationOnScreen(),
                    bounds.getSize()
                ));

            javax.imageio.ImageIO.write(
                screenshot,
                "PNG",
                new java.io.File(dir, name + ".png")
            );
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }

    /**
     * Wait for a specific amount of milliseconds.
     * Useful for debugging or waiting for animations.
     */
    protected void waitFor(int milliseconds)
    {
        robot().waitForIdle();
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Find an InternalDialog (JInternalFrame) by title.
     * DD Poker uses InternalDialog instead of JDialog, so this matcher is essential for dialog tests.
     *
     * @param titleContains Text that the dialog title should contain
     * @return The found JInternalFrame (which is an InternalDialog)
     * @throws org.assertj.swing.exception.ComponentLookupException if dialog not found
     */
    protected JInternalFrame findInternalDialog(String titleContains)
    {
        robot().waitForIdle();
        return robot().finder().find(PokerMatchers.internalDialogWithTitle(titleContains));
    }

    /**
     * Wait for a condition to become true, polling at regular intervals.
     * Useful for waiting on asynchronous UI updates.
     *
     * @param condition The condition to wait for
     * @param timeoutMs Maximum time to wait in milliseconds
     * @param description Description of what we're waiting for (used in error messages)
     * @return true if condition was met, false if timeout occurred
     */
    protected boolean waitForCondition(BooleanSupplier condition, long timeoutMs, String description)
    {
        long startTime = System.currentTimeMillis();
        long pollInterval = 100; // Poll every 100ms

        while (System.currentTimeMillis() - startTime < timeoutMs)
        {
            robot().waitForIdle();

            if (condition.getAsBoolean())
            {
                return true;
            }

            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        System.err.println("Timeout waiting for: " + description);
        return false;
    }

    /**
     * Print the component hierarchy for debugging purposes.
     * Dumps the entire Swing component tree to System.out, showing component types and names.
     * Useful for understanding the UI structure when writing tests.
     */
    protected void printComponentHierarchy()
    {
        System.out.println("\n=== Component Hierarchy ===");
        printComponentTree(window.target(), 0);
        System.out.println("=========================\n");
    }

    /**
     * Recursive helper to print component tree.
     *
     * @param component The component to print
     * @param level Indentation level
     */
    private void printComponentTree(Component component, int level)
    {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++)
        {
            indent.append("  ");
        }

        String name = component.getName();
        String nameInfo = (name != null && !name.isEmpty()) ? " [" + name + "]" : "";
        String visibleInfo = component.isVisible() ? "" : " (hidden)";

        System.out.println(indent + component.getClass().getSimpleName() + nameInfo + visibleInfo);

        if (component instanceof Container)
        {
            Container container = (Container) component;
            for (Component child : container.getComponents())
            {
                printComponentTree(child, level + 1);
            }
        }
    }
}
