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
package com.donohoedigital.games.poker.logic;

import com.donohoedigital.games.poker.logic.LevelTransitionLogic.MessageKeyContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LevelTransitionLogic - level transition logic extracted from
 * NewLevelActions.java. Tests run in headless mode with no UI dependencies.
 * Part of Wave 2 testability refactoring.
 */
@Tag("unit")
class LevelTransitionLogicTest {

    // =================================================================
    // determineBreakMessageKey() Tests
    // =================================================================

    @Test
    void should_ReturnChatKey_When_OnlineBreak() {
        String key = LevelTransitionLogic.determineBreakMessageKey(true);

        assertThat(key).isEqualTo("msg.chat.break");
    }

    @Test
    void should_ReturnDialogKey_When_PracticeBreak() {
        String key = LevelTransitionLogic.determineBreakMessageKey(false);

        assertThat(key).isEqualTo("msg.dialog.break");
    }

    // =================================================================
    // determineLevelMessageKey() Tests
    // =================================================================

    @Test
    void should_ReturnChatAnteKey_When_OnlineWithAnte() {
        String key = LevelTransitionLogic.determineLevelMessageKey(true, true);

        assertThat(key).isEqualTo("msg.chat.next.ante");
    }

    @Test
    void should_ReturnChatKey_When_OnlineWithoutAnte() {
        String key = LevelTransitionLogic.determineLevelMessageKey(true, false);

        assertThat(key).isEqualTo("msg.chat.next");
    }

    @Test
    void should_ReturnDialogAnteKey_When_PracticeWithAnte() {
        String key = LevelTransitionLogic.determineLevelMessageKey(false, true);

        assertThat(key).isEqualTo("msg.dialog.next.ante");
    }

    @Test
    void should_ReturnDialogKey_When_PracticeWithoutAnte() {
        String key = LevelTransitionLogic.determineLevelMessageKey(false, false);

        assertThat(key).isEqualTo("msg.dialog.next");
    }

    // =================================================================
    // determineTransitionMessageKey() Tests
    // =================================================================

    @Test
    void should_ReturnBreakChatKey_When_OnlineBreak() {
        MessageKeyContext ctx = new MessageKeyContext(true, true, false);

        String key = LevelTransitionLogic.determineTransitionMessageKey(ctx);

        assertThat(key).isEqualTo("msg.chat.break");
    }

    @Test
    void should_ReturnBreakDialogKey_When_PracticeBreak() {
        MessageKeyContext ctx = new MessageKeyContext(true, false, false);

        String key = LevelTransitionLogic.determineTransitionMessageKey(ctx);

        assertThat(key).isEqualTo("msg.dialog.break");
    }

    @Test
    void should_ReturnLevelChatAnteKey_When_OnlineLevelWithAnte() {
        MessageKeyContext ctx = new MessageKeyContext(false, true, true);

        String key = LevelTransitionLogic.determineTransitionMessageKey(ctx);

        assertThat(key).isEqualTo("msg.chat.next.ante");
    }

    @Test
    void should_ReturnLevelChatKey_When_OnlineLevelWithoutAnte() {
        MessageKeyContext ctx = new MessageKeyContext(false, true, false);

        String key = LevelTransitionLogic.determineTransitionMessageKey(ctx);

        assertThat(key).isEqualTo("msg.chat.next");
    }

    @Test
    void should_ReturnLevelDialogAnteKey_When_PracticeLevelWithAnte() {
        MessageKeyContext ctx = new MessageKeyContext(false, false, true);

        String key = LevelTransitionLogic.determineTransitionMessageKey(ctx);

        assertThat(key).isEqualTo("msg.dialog.next.ante");
    }

    @Test
    void should_ReturnLevelDialogKey_When_PracticeLevelWithoutAnte() {
        MessageKeyContext ctx = new MessageKeyContext(false, false, false);

        String key = LevelTransitionLogic.determineTransitionMessageKey(ctx);

        assertThat(key).isEqualTo("msg.dialog.next");
    }

    // =================================================================
    // isPlayerEligibleForRebuy() Tests
    // =================================================================

    @Test
    void should_BeEligible_When_NotObserverNotEliminated() {
        boolean eligible = LevelTransitionLogic.isPlayerEligibleForRebuy(false, false);

        assertThat(eligible).isTrue();
    }

    @Test
    void should_NotBeEligible_When_Observer() {
        boolean eligible = LevelTransitionLogic.isPlayerEligibleForRebuy(true, false);

        assertThat(eligible).isFalse();
    }

    @Test
    void should_NotBeEligible_When_Eliminated() {
        boolean eligible = LevelTransitionLogic.isPlayerEligibleForRebuy(false, true);

        assertThat(eligible).isFalse();
    }

    @Test
    void should_NotBeEligible_When_ObserverAndEliminated() {
        boolean eligible = LevelTransitionLogic.isPlayerEligibleForRebuy(true, true);

        assertThat(eligible).isFalse();
    }
}
