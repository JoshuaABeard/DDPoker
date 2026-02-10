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
package com.donohoedigital.games.poker.ui;

import com.donohoedigital.config.ConfigTestHelper;
import com.donohoedigital.games.poker.PokerMain;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;

import javax.swing.*;
import java.awt.*;

/**
 * Base class for UI tests using AssertJ Swing.
 * Provides common setup/teardown and utilities for testing the DD Poker Swing application.
 */
public abstract class PokerUITestBase extends AssertJSwingJUnitTestCase
{
    protected FrameFixture window;
    protected PokerMain pokerMain;

    /**
     * Set up the robot and launch the application.
     */
    @Override
    protected void onSetUp()
    {
        // Initialize config for poker with GUI support
        ConfigTestHelper.initializeWithGuiForTesting("poker");

        // Launch the application on the EDT
        SwingUtilities.invokeLater(() -> {
            try {
                pokerMain = new PokerMain("poker", "poker", new String[]{"-test"});
                pokerMain.init();
            } catch (Exception e) {
                throw new RuntimeException("Failed to start PokerMain", e);
            }
        });

        // Wait for and capture the main frame
        JFrame mainFrame = findMainFrame(robot());
        window = new FrameFixture(robot(), mainFrame);
        window.show(); // Brings frame to front
    }

    /**
     * Clean up resources after each test.
     */
    @Override
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
     * Find the main DD Poker frame.
     * Uses a matcher to find the frame by title or type.
     */
    private JFrame findMainFrame(Robot robot)
    {
        return robot.finder().find(new GenericTypeMatcher<JFrame>(JFrame.class) {
            @Override
            protected boolean isMatching(JFrame frame) {
                String title = frame.getTitle();
                return title != null && (title.contains("DD Poker") || title.contains("poker"));
            }
        });
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
}
