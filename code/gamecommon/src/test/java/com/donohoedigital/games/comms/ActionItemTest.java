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
package com.donohoedigital.games.comms;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ActionItem - action tracking with player participation and
 * timestamps.
 */
class ActionItemTest {

    // ========== Constructor Tests ==========

    @Test
    void should_SetCreateTimestamp_When_DefaultConstructorUsed() {
        ActionItem item = new ActionItem();

        assertThat(item.getCreateTimeStamp()).isGreaterThan(0);
    }

    @Test
    void should_SetActionID_When_ParameterizedConstructorUsed() {
        ActionItem item = new ActionItem(42);

        assertThat(item.getActionID()).isEqualTo(42);
        assertThat(item.getCreateTimeStamp()).isGreaterThan(0);
    }

    // ========== ActionID Tests ==========

    @Test
    void should_ReturnActionID_When_SetAndGet() {
        ActionItem item = new ActionItem();

        item.setActionID(99);

        assertThat(item.getActionID()).isEqualTo(99);
    }

    // ========== Timestamp Tests ==========

    @Test
    void should_SetNewTimestamp_When_SetCreateTimeStampCalled() {
        ActionItem item = new ActionItem();
        long first = item.getCreateTimeStamp();

        // Force a new timestamp
        item.setCreateTimeStamp();
        long second = item.getCreateTimeStamp();

        assertThat(second).isGreaterThanOrEqualTo(first);
    }

    @Test
    void should_ReturnDate_When_GetCreateDateCalled() {
        ActionItem item = new ActionItem();

        Date date = item.getCreateDate();

        assertThat(date).isNotNull();
        // Date should be roughly "now"
        assertThat(date.getTime()).isCloseTo(System.currentTimeMillis(), within(5000L));
    }

    // ========== Player Tracking Tests ==========

    @Test
    void should_TrackPlayer_When_AddPlayerCalled() {
        ActionItem item = new ActionItem(1);

        item.addPlayer(0);

        assertThat(item.isPlayerActionRequired(0)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_PlayerNotAdded() {
        ActionItem item = new ActionItem(1);

        assertThat(item.isPlayerActionRequired(5)).isFalse();
    }

    @Test
    void should_RemovePlayer_When_RemovePlayerCalled() {
        ActionItem item = new ActionItem(1);
        item.addPlayer(0);

        item.removePlayer(0);

        assertThat(item.isPlayerActionRequired(0)).isFalse();
    }

    @Test
    void should_ReturnFalse_When_PlayerHasNotActed() {
        ActionItem item = new ActionItem(1);
        item.addPlayer(0);

        assertThat(item.hasPlayerActed(0)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_SetPlayerActedCalled() {
        ActionItem item = new ActionItem(1);
        item.addPlayer(0);

        boolean result = item.setPlayerActed(0);

        assertThat(result).isTrue();
        assertThat(item.hasPlayerActed(0)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_SetPlayerActedOnNonParticipant() {
        ActionItem item = new ActionItem(1);

        boolean result = item.setPlayerActed(5);

        assertThat(result).isFalse();
    }

    @Test
    void should_ReturnFalse_When_SetPlayerActedCalledTwice() {
        ActionItem item = new ActionItem(1);
        item.addPlayer(0);
        item.setPlayerActed(0);

        boolean result = item.setPlayerActed(0);

        assertThat(result).isFalse();
    }

    // ========== isDone Tests ==========

    @Test
    void should_ReturnTrue_When_AllPlayersHaveActed() {
        ActionItem item = new ActionItem(1);
        item.addPlayer(0);
        item.addPlayer(1);

        item.setPlayerActed(0);
        item.setPlayerActed(1);

        assertThat(item.isDone()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NotAllPlayersHaveActed() {
        ActionItem item = new ActionItem(1);
        item.addPlayer(0);
        item.addPlayer(1);

        item.setPlayerActed(0);

        assertThat(item.isDone()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_NoPlayersAdded() {
        ActionItem item = new ActionItem(1);

        assertThat(item.isDone()).isTrue();
    }

    // ========== RemindEmail Tests ==========

    @Test
    void should_ReturnFalse_When_RemindEmailNotSet() {
        ActionItem item = new ActionItem();

        assertThat(item.getRemindEmailSent()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_RemindEmailSetTrue() {
        ActionItem item = new ActionItem();

        item.setRemindEmailSent(true);

        assertThat(item.getRemindEmailSent()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_RemindEmailSetFalse() {
        ActionItem item = new ActionItem();
        item.setRemindEmailSent(true);

        item.setRemindEmailSent(false);

        assertThat(item.getRemindEmailSent()).isFalse();
    }

    // ========== hasPlayerActed Error Handling ==========

    @Test
    void should_ThrowException_When_HasPlayerActedCalledOnNonParticipant() {
        ActionItem item = new ActionItem(1);

        assertThatThrownBy(() -> item.hasPlayerActed(5)).hasMessageContaining("player not part of this action");
    }
}
