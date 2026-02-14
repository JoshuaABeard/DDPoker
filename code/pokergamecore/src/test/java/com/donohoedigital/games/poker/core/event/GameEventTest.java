/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.core.event;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.core.state.TableState;

/**
 * Tests for {@link GameEvent} sealed interface and its record implementations.
 */
class GameEventTest {

    @Test
    void handStarted_shouldCreateCorrectly() {
        GameEvent event = new GameEvent.HandStarted(1, 10);

        assertThat(event).isInstanceOf(GameEvent.HandStarted.class);
        GameEvent.HandStarted handStarted = (GameEvent.HandStarted) event;
        assertThat(handStarted.tableId()).isEqualTo(1);
        assertThat(handStarted.handNumber()).isEqualTo(10);
    }

    @Test
    void playerActed_shouldCreateCorrectly() {
        GameEvent event = new GameEvent.PlayerActed(1, 5, ActionType.CALL, 100);

        assertThat(event).isInstanceOf(GameEvent.PlayerActed.class);
        GameEvent.PlayerActed playerActed = (GameEvent.PlayerActed) event;
        assertThat(playerActed.tableId()).isEqualTo(1);
        assertThat(playerActed.playerId()).isEqualTo(5);
        assertThat(playerActed.action()).isEqualTo(ActionType.CALL);
        assertThat(playerActed.amount()).isEqualTo(100);
    }

    @Test
    void communityCardsDealt_shouldCreateCorrectly() {
        GameEvent event = new GameEvent.CommunityCardsDealt(1, BettingRound.FLOP);

        assertThat(event).isInstanceOf(GameEvent.CommunityCardsDealt.class);
        GameEvent.CommunityCardsDealt cardsDealt = (GameEvent.CommunityCardsDealt) event;
        assertThat(cardsDealt.tableId()).isEqualTo(1);
        assertThat(cardsDealt.round()).isEqualTo(BettingRound.FLOP);
    }

    @Test
    void tableStateChanged_shouldCreateCorrectly() {
        GameEvent event = new GameEvent.TableStateChanged(1, TableState.BETTING, TableState.COMMUNITY);

        assertThat(event).isInstanceOf(GameEvent.TableStateChanged.class);
        GameEvent.TableStateChanged stateChanged = (GameEvent.TableStateChanged) event;
        assertThat(stateChanged.tableId()).isEqualTo(1);
        assertThat(stateChanged.oldState()).isEqualTo(TableState.BETTING);
        assertThat(stateChanged.newState()).isEqualTo(TableState.COMMUNITY);
    }

    @Test
    void potAwarded_shouldCreateCorrectly() {
        int[] winners = {1, 3, 5};
        GameEvent event = new GameEvent.PotAwarded(1, 0, winners, 1000);

        assertThat(event).isInstanceOf(GameEvent.PotAwarded.class);
        GameEvent.PotAwarded potAwarded = (GameEvent.PotAwarded) event;
        assertThat(potAwarded.tableId()).isEqualTo(1);
        assertThat(potAwarded.potIndex()).isEqualTo(0);
        assertThat(potAwarded.winnerIds()).containsExactly(1, 3, 5);
        assertThat(potAwarded.amount()).isEqualTo(1000);
    }

    @Test
    void records_shouldSupportEquality() {
        GameEvent event1 = new GameEvent.HandStarted(1, 10);
        GameEvent event2 = new GameEvent.HandStarted(1, 10);
        GameEvent event3 = new GameEvent.HandStarted(1, 11);

        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
    }

    @Test
    void records_shouldSupportToString() {
        GameEvent event = new GameEvent.HandStarted(1, 10);

        assertThat(event.toString()).contains("HandStarted");
        assertThat(event.toString()).contains("tableId=1");
        assertThat(event.toString()).contains("handNumber=10");
    }

    @Test
    void patternMatching_shouldWorkWithSealedInterface() {
        GameEvent event = new GameEvent.PlayerActed(1, 5, ActionType.FOLD, 0);

        String description =
                switch (event) {
                    case GameEvent.HandStarted e -> "Hand #" + e.handNumber() + " started";
                    case GameEvent.PlayerActed e -> "Player " + e.playerId() + " " + e.action();
                    case GameEvent.HandCompleted e -> "Hand completed";
                    default -> "Other event";
                };

        assertThat(description).isEqualTo("Player 5 FOLD");
    }
}
