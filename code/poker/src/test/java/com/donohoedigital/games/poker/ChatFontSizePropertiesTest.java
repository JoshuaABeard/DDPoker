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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.engine.PokerConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for chat font size option properties in client.properties
 */
class ChatFontSizePropertiesTest {

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for tests (only once)
        if (!PropertyConfig.isInitialized()) {
            new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        }
    }

    @Test
    void should_HaveLabelProperty() {
        String label = PropertyConfig.getRequiredStringProperty(
            "option." + PokerConstants.OPTION_CHAT_FONT_SIZE + ".label");

        assertThat(label)
            .isNotNull()
            .isNotEmpty()
            .contains("Font Size");
    }

    @Test
    void should_HaveDefaultProperty() {
        String defaultValue = PropertyConfig.getRequiredStringProperty(
            "option." + PokerConstants.OPTION_CHAT_FONT_SIZE + ".default");

        assertThat(defaultValue)
            .isNotNull()
            .isEqualTo("12");
    }

    @Test
    void should_HaveHelpProperty() {
        String help = PropertyConfig.getRequiredStringProperty(
            "option." + PokerConstants.OPTION_CHAT_FONT_SIZE + ".help");

        assertThat(help)
            .isNotNull()
            .isNotEmpty()
            .containsIgnoringCase("font size")
            .containsIgnoringCase("chat");
    }

    @Test
    void should_HaveDefaultValueMatchingConstant() {
        String defaultValue = PropertyConfig.getRequiredStringProperty(
            "option." + PokerConstants.OPTION_CHAT_FONT_SIZE + ".default");

        int defaultInt = Integer.parseInt(defaultValue);
        assertThat(defaultInt).isEqualTo(PokerConstants.DEFAULT_CHAT_FONT_SIZE);
    }

    @Test
    void should_HaveHelpTextMentioningRange() {
        String help = PropertyConfig.getRequiredStringProperty(
            "option." + PokerConstants.OPTION_CHAT_FONT_SIZE + ".help");

        // Help should mention the valid range (8-24)
        assertThat(help)
            .matches(".*\\b8-?24\\b.*");
    }
}
