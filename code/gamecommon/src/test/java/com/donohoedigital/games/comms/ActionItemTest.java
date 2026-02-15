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

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.comms.DMTypedHashMap;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ActionItem - tracks player actions in the game
 */
class ActionItemTest {

    // =================================================================
    // Test Helper Classes
    // =================================================================

    private static class TestGameInfo implements GameInfo {
        private final int numPlayers;
        private final boolean[] eliminated;

        TestGameInfo(int numPlayers) {
            this.numPlayers = numPlayers;
            this.eliminated = new boolean[numPlayers];
        }

        void setEliminated(int id, boolean eliminated) {
            this.eliminated[id] = eliminated;
        }

        @Override
        public int getNumPlayers() {
            return numPlayers;
        }

        @Override
        public boolean isEliminated(int id) {
            return eliminated[id];
        }

        // Unused methods for ActionItem tests
        @Override
        public int getPlayerIdAt(int i) {
            return i;
        }

        @Override
        public DMTypedHashMap getGameOptions() {
            return new DMTypedHashMap();
        }

        @Override
        public int getTurn() {
            return 0;
        }

        @Override
        public void setTurn(int n) {
        }
    }

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateActionItem_When_DefaultConstructorUsed() {
        ActionItem item = new ActionItem();

        assertThat(item).isNotNull();
        assertThat(item.getCreateTimeStamp()).isGreaterThan(0);
    }

    @Test
    void should_CreateActionItemWithId_When_IdConstructorUsed() {
        ActionItem item = new ActionItem(42);

        assertThat(item.getActionID()).isEqualTo(42);
        assertThat(item.getCreateTimeStamp()).isGreaterThan(0);
    }

    @Test
    void should_SetTimestamp_When_ActionItemCreated() {
        long before = System.currentTimeMillis() * 1000;
        ActionItem item = new ActionItem();
        long after = (System.currentTimeMillis() + 1) * 1000; // Add 1ms buffer for sequence number

        assertThat(item.getCreateTimeStamp()).isBetween(before, after);
    }

    // =================================================================
    // Action ID Tests
    // =================================================================

    @Test
    void should_ReturnActionId_When_SetActionIdCalled() {
        ActionItem item = new ActionItem();
        item.setActionID(123);

        assertThat(item.getActionID()).isEqualTo(123);
    }

    @Test
    void should_UpdateActionId_When_SetActionIdCalledMultipleTimes() {
        ActionItem item = new ActionItem(10);

        item.setActionID(20);
        assertThat(item.getActionID()).isEqualTo(20);

        item.setActionID(30);
        assertThat(item.getActionID()).isEqualTo(30);
    }

    // =================================================================
    // Timestamp Tests
    // =================================================================

    @Test
    void should_UpdateTimestamp_When_SetCreateTimeStampCalled() {
        ActionItem item = new ActionItem();
        long original = item.getCreateTimeStamp();

        // Small delay to ensure different timestamp
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        item.setCreateTimeStamp();
        long updated = item.getCreateTimeStamp();

        assertThat(updated).isGreaterThan(original);
    }

    @Test
    void should_ReturnDate_When_GetCreateDateCalled() {
        ActionItem item = new ActionItem();
        Date date = item.getCreateDate();

        assertThat(date).isNotNull();
        assertThat(date.getTime()).isCloseTo(System.currentTimeMillis(), within(1000L));
    }

    @Test
    void should_ReturnNull_When_GetCreateDateCalledWithNoTimestamp() {
        ActionItem item = new ActionItem();
        // Manually clear the timestamp
        item.removeLong(ActionItem.PARAM_CREATE);

        assertThat(item.getCreateDate()).isNull();
        assertThat(item.getCreateTimeStamp()).isEqualTo(0);
    }

    // =================================================================
    // Remind Email Tests
    // =================================================================

    @Test
    void should_ReturnFalse_When_RemindEmailNotSet() {
        ActionItem item = new ActionItem();

        assertThat(item.getRemindEmailSent()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_RemindEmailSetToTrue() {
        ActionItem item = new ActionItem();
        item.setRemindEmailSent(true);

        assertThat(item.getRemindEmailSent()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_RemindEmailSetToFalse() {
        ActionItem item = new ActionItem();
        item.setRemindEmailSent(false);

        assertThat(item.getRemindEmailSent()).isFalse();
    }

    @Test
    void should_ToggleRemindEmail_When_SetMultipleTimes() {
        ActionItem item = new ActionItem();

        item.setRemindEmailSent(true);
        assertThat(item.getRemindEmailSent()).isTrue();

        item.setRemindEmailSent(false);
        assertThat(item.getRemindEmailSent()).isFalse();

        item.setRemindEmailSent(true);
        assertThat(item.getRemindEmailSent()).isTrue();
    }

    // =================================================================
    // Player Management Tests
    // =================================================================

    @Test
    void should_AddPlayer_When_AddPlayerCalled() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);

        assertThat(item.isPlayerActionRequired(0)).isTrue();
    }

    @Test
    void should_AddMultiplePlayers_When_AddPlayerCalledMultipleTimes() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);
        item.addPlayer(1);
        item.addPlayer(2);

        assertThat(item.isPlayerActionRequired(0)).isTrue();
        assertThat(item.isPlayerActionRequired(1)).isTrue();
        assertThat(item.isPlayerActionRequired(2)).isTrue();
    }

    @Test
    void should_AddAllPlayers_When_AddAllPlayersCalled() {
        TestGameInfo game = new TestGameInfo(4);
        ActionItem item = new ActionItem();

        item.addAllPlayers(game);

        assertThat(item.isPlayerActionRequired(0)).isTrue();
        assertThat(item.isPlayerActionRequired(1)).isTrue();
        assertThat(item.isPlayerActionRequired(2)).isTrue();
        assertThat(item.isPlayerActionRequired(3)).isTrue();
    }

    @Test
    void should_AddOnlyNonEliminatedPlayers_When_AddNonEliminatedPlayersCalled() {
        TestGameInfo game = new TestGameInfo(4);
        game.setEliminated(1, true);
        game.setEliminated(3, true);

        ActionItem item = new ActionItem();
        item.addNonEliminatedPlayers(game);

        assertThat(item.isPlayerActionRequired(0)).isTrue();
        assertThat(item.isPlayerActionRequired(1)).isFalse();
        assertThat(item.isPlayerActionRequired(2)).isTrue();
        assertThat(item.isPlayerActionRequired(3)).isFalse();
    }

    @Test
    void should_RemovePlayer_When_RemovePlayerCalled() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);
        item.addPlayer(1);

        item.removePlayer(0);

        assertThat(item.isPlayerActionRequired(0)).isFalse();
        assertThat(item.isPlayerActionRequired(1)).isTrue();
    }

    @Test
    void should_RemoveAllPlayers_When_RemoveAllPlayersCalled() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);
        item.addPlayer(1);
        item.addPlayer(2);

        item.removeAllPlayers(3);

        assertThat(item.isPlayerActionRequired(0)).isFalse();
        assertThat(item.isPlayerActionRequired(1)).isFalse();
        assertThat(item.isPlayerActionRequired(2)).isFalse();
    }

    @Test
    void should_SetSinglePlayer_When_SetPlayerCalled() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);
        item.addPlayer(1);
        item.addPlayer(2);

        item.setPlayer(1, 3);

        assertThat(item.isPlayerActionRequired(0)).isFalse();
        assertThat(item.isPlayerActionRequired(1)).isTrue();
        assertThat(item.isPlayerActionRequired(2)).isFalse();
    }

    // =================================================================
    // Player Action Status Tests
    // =================================================================

    @Test
    void should_ReturnFalse_When_IsPlayerActionRequiredForNonAddedPlayer() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);

        assertThat(item.isPlayerActionRequired(1)).isFalse();
    }

    @Test
    void should_ReturnFalse_When_HasPlayerActedForNewPlayer() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);

        assertThat(item.hasPlayerActed(0)).isFalse();
    }

    @Test
    void should_ThrowException_When_HasPlayerActedForNonAddedPlayer() {
        ActionItem item = new ActionItem(1);
        item.addPlayer(0);

        assertThatThrownBy(() -> item.hasPlayerActed(1)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("Asking if player acted, but player not part of this action: 1");
    }

    @Test
    void should_MarkPlayerAsActed_When_SetPlayerActedCalled() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);

        boolean result = item.setPlayerActed(0);

        assertThat(result).isTrue();
        assertThat(item.hasPlayerActed(0)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_SetPlayerActedForNonAddedPlayer() {
        ActionItem item = new ActionItem(1);

        boolean result = item.setPlayerActed(0);

        assertThat(result).isFalse();
    }

    @Test
    void should_ReturnFalse_When_SetPlayerActedForAlreadyActedPlayer() {
        ActionItem item = new ActionItem(1);
        item.addPlayer(0);
        item.setPlayerActed(0);

        boolean result = item.setPlayerActed(0);

        assertThat(result).isFalse();
    }

    @Test
    void should_ReturnActedDate_When_GetPlayerActedDateCalled() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);
        item.setPlayerActed(0);

        Date date = item.getPlayerActedDate(0);

        assertThat(date).isNotNull();
        assertThat(date.getTime()).isCloseTo(System.currentTimeMillis(), within(1000L));
    }

    @Test
    void should_ReturnNull_When_GetPlayerActedDateForNonAddedPlayer() {
        ActionItem item = new ActionItem();

        assertThat(item.getPlayerActedDate(0)).isNull();
    }

    // =================================================================
    // Completion Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_IsDoneWithNoPlayers() {
        ActionItem item = new ActionItem();

        assertThat(item.isDone()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_IsDoneWithPlayersWhoHaventActed() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);
        item.addPlayer(1);

        assertThat(item.isDone()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_IsDoneWithSomePlayersWhoActed() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);
        item.addPlayer(1);
        item.setPlayerActed(0);

        assertThat(item.isDone()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_IsDoneWithAllPlayersActed() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);
        item.addPlayer(1);
        item.setPlayerActed(0);
        item.setPlayerActed(1);

        assertThat(item.isDone()).isTrue();
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_HandleLargePlayerIds_When_PlayersAdded() {
        ActionItem item = new ActionItem();
        item.addPlayer(10);
        item.addPlayer(99);

        assertThat(item.isPlayerActionRequired(10)).isTrue();
        assertThat(item.isPlayerActionRequired(99)).isTrue();
    }

    @Test
    void should_HandlePlayerIdZero_When_PlayersAdded() {
        ActionItem item = new ActionItem();
        item.addPlayer(0);

        assertThat(item.isPlayerActionRequired(0)).isTrue();
        assertThat(item.hasPlayerActed(0)).isFalse();
    }

    @Test
    void should_HandleAllPlayerIds0to9_When_PlayersAdded() {
        ActionItem item = new ActionItem();
        for (int i = 0; i < 10; i++) {
            item.addPlayer(i);
        }

        for (int i = 0; i < 10; i++) {
            assertThat(item.isPlayerActionRequired(i)).isTrue();
            assertThat(item.hasPlayerActed(i)).isFalse();
        }
    }

    // =================================================================
    // Complex Scenario Tests
    // =================================================================

    @Test
    void should_TrackCompletePlayerActionSequence_When_ComplexScenario() {
        TestGameInfo game = new TestGameInfo(4);
        game.setEliminated(2, true);

        ActionItem item = new ActionItem(100);
        item.setRemindEmailSent(false);

        // Add only non-eliminated players
        item.addNonEliminatedPlayers(game);

        assertThat(item.getActionID()).isEqualTo(100);
        assertThat(item.isPlayerActionRequired(0)).isTrue();
        assertThat(item.isPlayerActionRequired(1)).isTrue();
        assertThat(item.isPlayerActionRequired(2)).isFalse();
        assertThat(item.isPlayerActionRequired(3)).isTrue();
        assertThat(item.isDone()).isFalse();

        // Player 0 acts
        item.setPlayerActed(0);
        assertThat(item.hasPlayerActed(0)).isTrue();
        assertThat(item.isDone()).isFalse();

        // Player 1 acts
        item.setPlayerActed(1);
        assertThat(item.hasPlayerActed(1)).isTrue();
        assertThat(item.isDone()).isFalse();

        // Player 3 acts
        item.setPlayerActed(3);
        assertThat(item.hasPlayerActed(3)).isTrue();
        assertThat(item.isDone()).isTrue();

        // Mark email sent
        item.setRemindEmailSent(true);
        assertThat(item.getRemindEmailSent()).isTrue();
    }

    @Test
    void should_MaintainIndependentTimestamps_When_MultiplePlayersAct() throws InterruptedException {
        ActionItem item = new ActionItem();
        item.addPlayer(0);
        item.addPlayer(1);

        item.setPlayerActed(0);
        Date date0 = item.getPlayerActedDate(0);

        Thread.sleep(2); // Small delay

        item.setPlayerActed(1);
        Date date1 = item.getPlayerActedDate(1);

        assertThat(date1).isAfter(date0);
    }

    @Test
    void should_PreserveActionIdAndTimestamp_When_PlayersModified() {
        ActionItem item = new ActionItem(999);
        long createTime = item.getCreateTimeStamp();
        int actionId = item.getActionID();

        item.addPlayer(0);
        item.removePlayer(0);
        item.addPlayer(1);

        assertThat(item.getActionID()).isEqualTo(actionId);
        assertThat(item.getCreateTimeStamp()).isEqualTo(createTime);
    }
}
