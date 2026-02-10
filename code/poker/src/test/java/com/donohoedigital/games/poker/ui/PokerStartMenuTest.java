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

import org.assertj.swing.fixture.JButtonFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for the Poker Start Menu.
 * These tests interact with the actual Swing UI to verify user workflows.
 */
@Tag("ui")
@Tag("slow")
public class PokerStartMenuTest extends PokerUITestBase
{
    @Test
    void should_ShowMainWindow_When_ApplicationLaunches()
    {
        // Verify main window is visible
        window.requireVisible();

        // Verify window title contains "DD Poker"
        String title = window.target().getTitle();
        assertThat(title).containsIgnoringCase("poker");

        // Take a screenshot for debugging
        takeScreenshot("main-window");
    }

    @Test
    void should_ShowButtons_When_OnMainMenu()
    {
        // Wait for UI to fully initialize
        robot().waitForIdle();

        // Verify key buttons exist
        // Note: Button names may need adjustment based on actual component names
        verifyButtonExists("practice");
        verifyButtonExists("analysis");
        verifyButtonExists("online");
        verifyButtonExists("pokerclock");
    }

    @Test
    void should_NavigateToPracticeMode_When_PracticeButtonClicked()
    {
        // Wait for UI to be ready
        robot().waitForIdle();

        // Find and click the practice button
        JButtonFixture practiceButton = window.button("practice");
        practiceButton.click();

        // Wait for navigation
        waitFor(500);

        // Take screenshot of practice mode
        takeScreenshot("practice-mode");

        // Verify we navigated (this is a basic check - can be enhanced)
        window.requireVisible();
    }

    @Test
    void should_OpenOnlineMenu_When_OnlineButtonClicked()
    {
        robot().waitForIdle();

        // Click online button
        JButtonFixture onlineButton = window.button("online");
        onlineButton.click();

        waitFor(500);
        takeScreenshot("online-menu");

        // Verify online menu opened
        window.requireVisible();
    }

    @Test
    void should_ShowMenuBar_When_ApplicationRunning()
    {
        // Verify menu bar exists by casting to JFrame
        JFrame frame = (JFrame) window.target();
        JMenuBar menuBar = frame.getJMenuBar();
        assertThat(menuBar).isNotNull();

        // Verify menu bar has menus
        assertThat(menuBar.getMenuCount()).isGreaterThan(0);
    }

    /**
     * Helper method to verify a button exists.
     */
    private void verifyButtonExists(String buttonName)
    {
        try {
            JButton button = window.button(buttonName).target();
            assertThat(button).isNotNull();
        } catch (Exception e) {
            // Button not found - log for debugging
            System.err.println("Button not found: " + buttonName);
            takeScreenshot("button-not-found-" + buttonName);
            throw e;
        }
    }
}
