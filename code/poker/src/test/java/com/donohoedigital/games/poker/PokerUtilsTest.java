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

import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerUtils - utility methods for poker game operations.
 */
class PokerUtilsTest {

    @BeforeEach
    void setUp() {
        ConfigManager configMgr = new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        configMgr.loadGuiConfig(); // Required for StylesConfig
    }

    // ========== getChipIcon() Tests ==========

    @Test
    void should_ReturnChip100kIcon_For100000Amount() {
        String icon = PokerUtils.getChipIcon(100000);

        assertThat(icon).isEqualTo("chip100k");
    }

    @Test
    void should_ReturnChip50kIcon_For50000Amount() {
        String icon = PokerUtils.getChipIcon(50000);

        assertThat(icon).isEqualTo("chip50k");
    }

    @Test
    void should_ReturnChip10kIcon_For10000Amount() {
        String icon = PokerUtils.getChipIcon(10000);

        assertThat(icon).isEqualTo("chip10k");
    }

    @Test
    void should_ReturnChip5kIcon_For5000Amount() {
        String icon = PokerUtils.getChipIcon(5000);

        assertThat(icon).isEqualTo("chip5k");
    }

    @Test
    void should_ReturnChip1kIcon_For1000Amount() {
        String icon = PokerUtils.getChipIcon(1000);

        assertThat(icon).isEqualTo("chip1k");
    }

    @Test
    void should_ReturnChip500Icon_For500Amount() {
        String icon = PokerUtils.getChipIcon(500);

        assertThat(icon).isEqualTo("chip500");
    }

    @Test
    void should_ReturnChip100Icon_For100Amount() {
        String icon = PokerUtils.getChipIcon(100);

        assertThat(icon).isEqualTo("chip100");
    }

    @Test
    void should_ReturnChip25Icon_For25Amount() {
        String icon = PokerUtils.getChipIcon(25);

        assertThat(icon).isEqualTo("chip25");
    }

    @Test
    void should_ReturnChip5Icon_For5Amount() {
        String icon = PokerUtils.getChipIcon(5);

        assertThat(icon).isEqualTo("chip5");
    }

    @Test
    void should_ReturnChip1Icon_For1Amount() {
        String icon = PokerUtils.getChipIcon(1);

        assertThat(icon).isEqualTo("chip1");
    }

    @Test
    void should_ReturnBlankIcon_ForNonStandardAmount() {
        assertThat(PokerUtils.getChipIcon(50)).isEqualTo("icon-blank");
        assertThat(PokerUtils.getChipIcon(10)).isEqualTo("icon-blank");
        assertThat(PokerUtils.getChipIcon(2)).isEqualTo("icon-blank");
        assertThat(PokerUtils.getChipIcon(0)).isEqualTo("icon-blank");
    }

    @Test
    void should_ReturnBlankIcon_ForNegativeAmount() {
        String icon = PokerUtils.getChipIcon(-100);

        assertThat(icon).isEqualTo("icon-blank");
    }

    // ========== getDisplaySeatForTerritory() Tests ==========
    // NOTE: Territory tests omitted - Territory requires full game initialization
    // and cannot be easily mocked. These would be better suited for integration
    // tests.

    // ========== Fold Key State Management Tests ==========

    @Test
    void should_InitializeFoldState_WhenNewHand() {
        PokerUtils.setNewHand();

        assertThat(PokerUtils.isFoldKey()).isFalse();
    }

    @Test
    void should_DisableFoldAcceptance_WhenNoFoldKeyCalled() {
        PokerUtils.setNewHand();
        PokerUtils.setNoFoldKey();

        // Fold key should no longer be accepted
        // (tested indirectly through setFoldKey behavior)
        assertThat(PokerUtils.isFoldKey()).isFalse();
    }

    @Test
    void should_MaintainFoldState_AcrossMultipleCalls() {
        PokerUtils.setNewHand();

        assertThat(PokerUtils.isFoldKey()).isFalse();
        assertThat(PokerUtils.isFoldKey()).isFalse(); // Still false
    }

    // ========== Math Delegation Tests ==========

    @Test
    void should_CalculatePower_ViaPokerLogicUtils() {
        assertThat(PokerUtils.pow(2, 0)).isEqualTo(1);
        assertThat(PokerUtils.pow(2, 1)).isEqualTo(2);
        assertThat(PokerUtils.pow(2, 3)).isEqualTo(8);
        assertThat(PokerUtils.pow(5, 2)).isEqualTo(25);
        assertThat(PokerUtils.pow(10, 3)).isEqualTo(1000);
    }

    @Test
    void should_HandlePowerOfZero() {
        assertThat(PokerUtils.pow(0, 0)).isEqualTo(1); // 0^0 = 1 by convention
        assertThat(PokerUtils.pow(0, 5)).isEqualTo(0);
    }

    @Test
    void should_CalculateNChooseK_ViaPokerLogicUtils() {
        assertThat(PokerUtils.nChooseK(5, 2)).isEqualTo(10); // 5 choose 2
        assertThat(PokerUtils.nChooseK(10, 3)).isEqualTo(120); // 10 choose 3
        assertThat(PokerUtils.nChooseK(52, 5)).isEqualTo(2598960); // 52 choose 5 (poker hands)
    }

    @Test
    void should_HandleNChooseKEdgeCases() {
        assertThat(PokerUtils.nChooseK(5, 0)).isEqualTo(1); // n choose 0 = 1
        assertThat(PokerUtils.nChooseK(5, 5)).isEqualTo(1); // n choose n = 1
        assertThat(PokerUtils.nChooseK(5, 1)).isEqualTo(5); // n choose 1 = n
    }

    // ========== Chat Formatting Tests ==========

    @Test
    void should_FormatImportantChatMessage() {
        String result = PokerUtils.chatImportant("Test message");

        assertThat(result).isNotNull();
        assertThat(result).contains("Test message");
    }

    @Test
    void should_FormatInformationChatMessage() {
        String result = PokerUtils.chatInformation("Info message");

        assertThat(result).isNotNull();
        assertThat(result).contains("Info message");
    }

    @Test
    void should_HandleEmptyString_InChatFormatting() {
        String important = PokerUtils.chatImportant("");
        String information = PokerUtils.chatInformation("");

        assertThat(important).isNotNull();
        assertThat(information).isNotNull();
    }

    @Test
    void should_HandleSpecialCharacters_InChatFormatting() {
        String message = "<b>HTML & Special</b>";

        String important = PokerUtils.chatImportant(message);
        String information = PokerUtils.chatInformation(message);

        assertThat(important).contains(message);
        assertThat(information).contains(message);
    }

    // ========== roundAmountMinChip Tests ==========

    @Test
    void roundAmountMinChip_returnsChipsUnchanged_whenMinChipIsZero() {
        PokerTable table = Mockito.mock(PokerTable.class);
        Mockito.when(table.getMinChip()).thenReturn(0);

        // Previously threw ArithmeticException (/ by zero); now returns chips unchanged
        assertThat(PokerUtils.roundAmountMinChip(table, 500)).isEqualTo(500);
        assertThat(PokerUtils.roundAmountMinChip(table, 0)).isEqualTo(0);
        assertThat(PokerUtils.roundAmountMinChip(table, 1000)).isEqualTo(1000);
    }

    @Test
    void roundAmountMinChip_roundsToNearestMultiple() {
        PokerTable table = Mockito.mock(PokerTable.class);
        Mockito.when(table.getMinChip()).thenReturn(25);

        assertThat(PokerUtils.roundAmountMinChip(table, 500)).isEqualTo(500); // exact
        assertThat(PokerUtils.roundAmountMinChip(table, 510)).isEqualTo(500); // round down
        assertThat(PokerUtils.roundAmountMinChip(table, 515)).isEqualTo(525); // round up (>= half)
        assertThat(PokerUtils.roundAmountMinChip(table, 525)).isEqualTo(525); // exact
    }

    // ========== Edge Case Tests ==========

    @Test
    void should_HandleLargeChipAmounts() {
        // Amounts larger than 100k should return blank icon
        String icon = PokerUtils.getChipIcon(1000000);

        assertThat(icon).isEqualTo("icon-blank");
    }

    @Test
    void should_HandleChipAmountBetweenStandards() {
        // Amount between standard chip values
        assertThat(PokerUtils.getChipIcon(75)).isEqualTo("icon-blank");
        assertThat(PokerUtils.getChipIcon(250)).isEqualTo("icon-blank");
        assertThat(PokerUtils.getChipIcon(750)).isEqualTo("icon-blank");
    }

}
