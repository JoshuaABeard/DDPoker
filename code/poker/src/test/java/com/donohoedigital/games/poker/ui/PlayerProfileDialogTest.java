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

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for dialogs in DD Poker.
 * Demonstrates how to interact with modal dialogs, text fields, and buttons.
 */
@Tag("ui")
@Tag("slow")
public class PlayerProfileDialogTest extends PokerUITestBase
{
    @Test
    @Disabled("Requires specific navigation to profile dialog - implement when navigation is mapped")
    void should_CreatePlayerProfile_When_SubmittingValidData()
    {
        // Navigate to profile creation (implementation depends on UI flow)
        // This is a template showing how dialog testing works

        // Find the dialog
        DialogFixture dialog = findDialogByTitle("Player Profile");

        // Enter player name
        JTextComponentFixture nameField = dialog.textBox("playerName");
        nameField.enterText("TestPlayer");

        // Take screenshot
        takeScreenshot("profile-dialog-filled");

        // Click OK button
        dialog.button("okButton").click();

        // Verify dialog closed
        robot().waitForIdle();
        assertThat(dialog.target().isShowing()).isFalse();
    }

    @Test
    @Disabled("Requires specific navigation - template for future implementation")
    void should_ShowValidationError_When_SubmittingEmptyName()
    {
        DialogFixture dialog = findDialogByTitle("Player Profile");

        // Leave name field empty and try to submit
        dialog.button("okButton").click();

        // Verify error message appears
        robot().waitForIdle();
        takeScreenshot("profile-validation-error");

        // Dialog should still be showing
        assertThat(dialog.target().isShowing()).isTrue();
    }

    @Test
    void should_FindDialogsByType_When_Using_GenericTypeMatcher()
    {
        // Example of finding any JDialog that matches criteria
        // This is useful when you don't know the exact title

        // Trigger some action that opens a dialog first...
        // (implementation depends on UI flow)

        // Example matcher:
        GenericTypeMatcher<JDialog> dialogMatcher = new GenericTypeMatcher<JDialog>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog dialog) {
                return dialog.isVisible() &&
                       dialog.getTitle() != null &&
                       dialog.getTitle().contains("Profile");
            }
        };

        // Then use:
        // JDialog dialog = robot().finder().find(dialogMatcher);
        // DialogFixture fixture = new DialogFixture(robot(), dialog);
    }

    /**
     * Helper method to find a dialog by title.
     */
    private DialogFixture findDialogByTitle(String title)
    {
        JDialog dialog = robot().finder().find(new GenericTypeMatcher<JDialog>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog dialog) {
                return dialog.isVisible() &&
                       title.equals(dialog.getTitle());
            }
        });
        return new DialogFixture(robot(), dialog);
    }
}
