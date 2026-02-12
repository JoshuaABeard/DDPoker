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
package com.donohoedigital.games.poker.ui.matchers;

import com.donohoedigital.gui.DDImageButton;
import com.donohoedigital.gui.DDLabel;
import com.donohoedigital.gui.InternalDialog;
import org.assertj.swing.core.GenericTypeMatcher;

import javax.swing.*;

/**
 * Custom AssertJ Swing matchers for DD Poker's custom Swing components. These
 * matchers help locate components in the UI during tests.
 */
public class PokerMatchers {
    /**
     * Matcher for DDImageButton by name. DDImageButton components use image icons
     * instead of text, so matching by name is essential.
     *
     * @param name
     *            The button name to match (set via setName())
     * @return A matcher that finds DDImageButton with the given name
     */
    public static GenericTypeMatcher<DDImageButton> ddImageButtonNamed(String name) {
        return new GenericTypeMatcher<DDImageButton>(DDImageButton.class) {
            @Override
            protected boolean isMatching(DDImageButton button) {
                return name.equals(button.getName());
            }

            @Override
            public String toString() {
                return "DDImageButton with name '" + name + "'";
            }
        };
    }

    /**
     * Matcher for InternalDialog (JInternalFrame) by title. DD Poker uses
     * InternalDialog (backed by JInternalFrame) instead of JDialog.
     *
     * @param titleContains
     *            Text that the dialog title should contain
     * @return A matcher that finds visible InternalDialog with matching title
     */
    public static GenericTypeMatcher<JInternalFrame> internalDialogWithTitle(String titleContains) {
        return new GenericTypeMatcher<JInternalFrame>(JInternalFrame.class) {
            @Override
            protected boolean isMatching(JInternalFrame frame) {
                return frame instanceof InternalDialog && frame.isVisible() && frame.getTitle() != null
                        && frame.getTitle().contains(titleContains);
            }

            @Override
            public String toString() {
                return "InternalDialog with title containing '" + titleContains + "'";
            }
        };
    }

    /**
     * Matcher for DDLabel by text content. Useful for finding labels that display
     * specific text.
     *
     * @param textContains
     *            Text that the label should contain
     * @return A matcher that finds DDLabel with matching text
     */
    public static GenericTypeMatcher<DDLabel> ddLabelWithText(String textContains) {
        return new GenericTypeMatcher<DDLabel>(DDLabel.class) {
            @Override
            protected boolean isMatching(DDLabel label) {
                String text = label.getText();
                return text != null && text.contains(textContains);
            }

            @Override
            public String toString() {
                return "DDLabel with text containing '" + textContains + "'";
            }
        };
    }
}
