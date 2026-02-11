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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * CRITICAL UI Smoke Tests
 *
 * Run these manually before releases to verify core UI functionality.
 * These tests launch the actual Swing application.
 *
 * To run: mvn test -Dtest=CriticalUISmokeTest -pl poker
 *
 * IMPORTANT: Requires a display. Will be automatically skipped in headless environments.
 */
@Tag("ui")
@Tag("smoke")
@Tag("manual")
public class CriticalUISmokeTest extends PokerUITestBase
{
    @Test
    void smoke_ApplicationLaunches()
    {
        // Verify main window appears
        window.requireVisible();
        takeScreenshot("smoke-app-launched");
    }

    @Test
    void smoke_CanNavigateToPracticeMode()
    {
        robot().waitForIdle();

        // Click practice button
        window.button("practice").click();
        robot().waitForIdle();

        waitFor(500);
        takeScreenshot("smoke-practice-mode");

        // Verify we navigated (basic check)
        window.requireVisible();
    }

    @Test
    void smoke_CanReturnToMainMenu()
    {
        robot().waitForIdle();

        // Navigate away
        window.button("practice").click();
        robot().waitForIdle();
        waitFor(500);

        // Navigate back
        window.button("cancelprev").click();
        robot().waitForIdle();
        waitFor(500);

        takeScreenshot("smoke-back-to-menu");

        // Verify we're back
        window.button("practice").requireVisible();
    }
}
