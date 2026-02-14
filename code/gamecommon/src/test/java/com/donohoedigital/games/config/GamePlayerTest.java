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
package com.donohoedigital.games.config;

import com.donohoedigital.comms.MsgState;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GamePlayer - Player representation and state management.
 */
class GamePlayerTest {

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyPlayer_When_DefaultConstructorUsed() {
        GamePlayer player = new GamePlayer();

        assertThat(player).isNotNull();
    }

    @Test
    void should_InitializeWithValues_When_ParameterizedConstructorUsed() {
        GamePlayer player = new GamePlayer(42, "TestPlayer");

        assertThat(player.getID()).isEqualTo(42);
        assertThat(player.getName()).isEqualTo("TestPlayer");
    }

    // ========== ID Management Tests ==========

    @Test
    void should_ReturnID_When_GetIDCalled() {
        GamePlayer player = new GamePlayer(5, "Player5");

        assertThat(player.getID()).isEqualTo(5);
        assertThat(player.getObjectID()).isEqualTo(5);
    }

    @Test
    void should_ReturnTrue_When_PlayerIsHost() {
        GamePlayer player = new GamePlayer(GamePlayer.HOST_ID, "Host");

        assertThat(player.isHost()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_PlayerIsNotHost() {
        GamePlayer player = new GamePlayer(1, "Player1");

        assertThat(player.isHost()).isFalse();
    }

    // ========== Name Management Tests ==========

    @Test
    void should_ReturnName_When_GetNameCalled() {
        GamePlayer player = new GamePlayer(1, "Alice");

        assertThat(player.getName()).isEqualTo("Alice");
        assertThat(player.toString()).isEqualTo("Alice");
    }

    @Test
    void should_UpdateName_When_SetNameCalled() {
        GamePlayer player = new GamePlayer(1, "Bob");

        player.setName("Robert");

        assertThat(player.getName()).isEqualTo("Robert");
    }

    // ========== State Management Tests ==========

    @Test
    void should_ReturnFalse_When_PlayerNotEliminated() {
        GamePlayer player = new GamePlayer(1, "Player1");

        assertThat(player.isEliminated()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_PlayerEliminated() {
        GamePlayer player = new GamePlayer(1, "Player1");

        player.setEliminated(true);

        assertThat(player.isEliminated()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NotCurrentPlayer() {
        GamePlayer player = new GamePlayer(1, "Player1");

        assertThat(player.isCurrentGamePlayer()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_SetAsCurrentPlayer() {
        GamePlayer player = new GamePlayer(1, "Player1");

        player.setCurrentGamePlayer(true);

        assertThat(player.isCurrentGamePlayer()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NotObserver() {
        GamePlayer player = new GamePlayer(1, "Player1");

        assertThat(player.isObserver()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_SetAsObserver() {
        GamePlayer player = new GamePlayer(1, "Player1");

        player.setObserver(true);

        assertThat(player.isObserver()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NotDirty() {
        GamePlayer player = new GamePlayer(1, "Player1");

        assertThat(player.isDirty()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_SetDirty() {
        GamePlayer player = new GamePlayer(1, "Player1");

        player.setDirty(true);

        assertThat(player.isDirty()).isTrue();
    }

    // ========== AI Management Tests ==========

    @Test
    void should_ReturnNull_When_NoAISet() {
        GamePlayer player = new GamePlayer(1, "Player1");

        assertThat(player.getGameAI()).isNull();
    }

    @Test
    void should_ReturnFalse_When_NoAISet() {
        GamePlayer player = new GamePlayer(1, "Player1");

        assertThat(player.isComputer()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_AISet() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestGameAI ai = new TestGameAI();

        player.setGameAI(ai);

        assertThat(player.isComputer()).isTrue();
        assertThat(player.getGameAI()).isSameAs(ai);
    }

    // ========== Last Poll Tests ==========

    @Test
    void should_ReturnZero_When_LastPollNotSet() {
        GamePlayer player = new GamePlayer(1, "Player1");

        assertThat(player.getLastPoll()).isZero();
    }

    @Test
    void should_ReturnValue_When_LastPollSet() {
        GamePlayer player = new GamePlayer(1, "Player1");
        long timestamp = System.currentTimeMillis();

        player.setLastPoll(timestamp);

        assertThat(player.getLastPoll()).isEqualTo(timestamp);
    }

    // ========== Info Map Tests ==========

    @Test
    void should_ReturnNull_When_InfoNotSet() {
        GamePlayer player = new GamePlayer(1, "Player1");

        assertThat(player.getInfo("key")).isNull();
    }

    @Test
    void should_ReturnValue_When_InfoSet() {
        GamePlayer player = new GamePlayer(1, "Player1");

        player.putInfo("score", 100);

        assertThat(player.getInfo("score")).isEqualTo(100);
    }

    @Test
    void should_RemoveValue_When_RemoveInfoCalled() {
        GamePlayer player = new GamePlayer(1, "Player1");
        player.putInfo("temp", "value");

        player.removeInfo("temp");

        assertThat(player.getInfo("temp")).isNull();
    }

    // ========== PropertyChangeListener Tests ==========

    @Test
    void should_NotifyListener_When_EliminationChanges() throws Exception {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPropertyListener listener = new TestPropertyListener();
        player.addPropertyChangeListener(listener);

        player.setEliminated(true);

        // Wait for event (async due to SwingUtilities.invokeLater)
        listener.awaitEvent(1000);

        assertThat(listener.events).hasSize(1);
        assertThat(listener.events.get(0).getPropertyName()).isEqualTo("eliminated");
        assertThat(listener.events.get(0).getNewValue()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void should_NotifyListener_When_InfoChanged() throws Exception {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPropertyListener listener = new TestPropertyListener();
        player.addPropertyChangeListener(listener);

        player.putInfo("score", 42);

        listener.awaitEvent(1000);

        assertThat(listener.events).hasSize(1);
        assertThat(listener.events.get(0).getPropertyName()).isEqualTo("score");
        assertThat(listener.events.get(0).getNewValue()).isEqualTo(42);
    }

    @Test
    void should_NotifyNamedListener_When_SpecificPropertyChanges() throws Exception {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPropertyListener scoreListener = new TestPropertyListener();
        player.addPropertyChangeListener("score", scoreListener);

        player.putInfo("score", 100);
        player.putInfo("level", 5);

        scoreListener.awaitEvent(1000);

        // Should only receive score event, not level event
        assertThat(scoreListener.events).hasSize(1);
        assertThat(scoreListener.events.get(0).getPropertyName()).isEqualTo("score");
    }

    @Test
    void should_NotNotifyListener_When_RemovedBeforeChange() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPropertyListener listener = new TestPropertyListener();
        player.addPropertyChangeListener(listener);

        player.removePropertyChangeListener(listener);
        player.setEliminated(true);

        assertThat(listener.events).isEmpty();
    }

    @Test
    void should_ReturnListeners_When_GetPropertyChangeListenersCalled() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPropertyListener listener = new TestPropertyListener();

        player.addPropertyChangeListener(listener);

        assertThat(player.getPropertyChangeListeners()).contains(listener);
    }

    @Test
    void should_ReturnNamedListeners_When_GetPropertyChangeListenersWithNameCalled() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPropertyListener listener = new TestPropertyListener();

        player.addPropertyChangeListener("score", listener);

        assertThat(player.getPropertyChangeListeners("score")).contains(listener);
    }

    // ========== Comparison Tests ==========

    @Test
    void should_CompareByID_When_CompareToCalled() {
        GamePlayer player1 = new GamePlayer(1, "Player1");
        GamePlayer player2 = new GamePlayer(2, "Player2");

        assertThat(player1.compareTo(player2)).isLessThan(0);
        assertThat(player2.compareTo(player1)).isGreaterThan(0);
        assertThat(player1.compareTo(player1)).isZero();
    }

    // ========== toString Tests ==========

    @Test
    void should_ReturnDetailedString_When_ToStringLongCalled() {
        GamePlayer player = new GamePlayer(1, "Alice");
        player.putInfo("score", 100);

        String result = player.toStringLong();

        assertThat(result).contains("Alice");
        assertThat(result).contains("id=1");
        assertThat(result).contains("score");
    }

    // ========== Test Helpers ==========

    /**
     * Simple GameAI stub for testing.
     */
    private static class TestGameAI extends GameAI {
        @Override
        public void demarshal(MsgState state, String sData) {
        }

        @Override
        public String marshal(MsgState state) {
            return "";
        }
    }

    /**
     * PropertyChangeListener that collects events for testing.
     */
    private static class TestPropertyListener implements PropertyChangeListener {
        final List<PropertyChangeEvent> events = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            events.add(evt);
            latch.countDown();
        }

        void awaitEvent(long timeoutMillis) throws InterruptedException {
            latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        }
    }
}
