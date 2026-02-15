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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.event.GameEvent.*;

/** Tests for all GameEvent record types. */
class GameEventRecordsTest {

    // PlayerAdded tests

    @Test
    void playerAdded_shouldCreateWithCorrectValues() {
        PlayerAdded event = new PlayerAdded(1, 101, 3);

        assertThat(event.tableId()).isEqualTo(1);
        assertThat(event.playerId()).isEqualTo(101);
        assertThat(event.seat()).isEqualTo(3);
    }

    @Test
    void playerAdded_shouldImplementEquality() {
        PlayerAdded event1 = new PlayerAdded(1, 101, 3);
        PlayerAdded event2 = new PlayerAdded(1, 101, 3);
        PlayerAdded event3 = new PlayerAdded(1, 101, 4);

        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    // PlayerRemoved tests

    @Test
    void playerRemoved_shouldCreateWithCorrectValues() {
        PlayerRemoved event = new PlayerRemoved(2, 202, 5);

        assertThat(event.tableId()).isEqualTo(2);
        assertThat(event.playerId()).isEqualTo(202);
        assertThat(event.seat()).isEqualTo(5);
    }

    @Test
    void playerRemoved_shouldImplementEquality() {
        PlayerRemoved event1 = new PlayerRemoved(2, 202, 5);
        PlayerRemoved event2 = new PlayerRemoved(2, 202, 5);

        assertThat(event1).isEqualTo(event2);
    }

    // LevelChanged tests

    @Test
    void levelChanged_shouldCreateWithCorrectValues() {
        LevelChanged event = new LevelChanged(1, 5);

        assertThat(event.tableId()).isEqualTo(1);
        assertThat(event.newLevel()).isEqualTo(5);
    }

    @Test
    void levelChanged_shouldImplementEquality() {
        LevelChanged event1 = new LevelChanged(1, 5);
        LevelChanged event2 = new LevelChanged(1, 5);
        LevelChanged event3 = new LevelChanged(1, 6);

        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
    }

    // ButtonMoved tests

    @Test
    void buttonMoved_shouldCreateWithCorrectValues() {
        ButtonMoved event = new ButtonMoved(1, 7);

        assertThat(event.tableId()).isEqualTo(1);
        assertThat(event.newSeat()).isEqualTo(7);
    }

    @Test
    void buttonMoved_shouldImplementEquality() {
        ButtonMoved event1 = new ButtonMoved(1, 7);
        ButtonMoved event2 = new ButtonMoved(1, 7);

        assertThat(event1).isEqualTo(event2);
    }

    // ShowdownStarted tests

    @Test
    void showdownStarted_shouldCreateWithCorrectValues() {
        ShowdownStarted event = new ShowdownStarted(3);

        assertThat(event.tableId()).isEqualTo(3);
    }

    @Test
    void showdownStarted_shouldImplementEquality() {
        ShowdownStarted event1 = new ShowdownStarted(3);
        ShowdownStarted event2 = new ShowdownStarted(3);
        ShowdownStarted event3 = new ShowdownStarted(4);

        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
    }

    // TournamentCompleted tests

    @Test
    void tournamentCompleted_shouldCreateWithCorrectValues() {
        TournamentCompleted event = new TournamentCompleted(999);

        assertThat(event.winnerId()).isEqualTo(999);
    }

    @Test
    void tournamentCompleted_shouldImplementEquality() {
        TournamentCompleted event1 = new TournamentCompleted(999);
        TournamentCompleted event2 = new TournamentCompleted(999);

        assertThat(event1).isEqualTo(event2);
    }

    // BreakStarted tests

    @Test
    void breakStarted_shouldCreateWithCorrectValues() {
        BreakStarted event = new BreakStarted(1);

        assertThat(event.tableId()).isEqualTo(1);
    }

    @Test
    void breakStarted_shouldImplementEquality() {
        BreakStarted event1 = new BreakStarted(1);
        BreakStarted event2 = new BreakStarted(1);

        assertThat(event1).isEqualTo(event2);
    }

    // BreakEnded tests

    @Test
    void breakEnded_shouldCreateWithCorrectValues() {
        BreakEnded event = new BreakEnded(1);

        assertThat(event.tableId()).isEqualTo(1);
    }

    @Test
    void breakEnded_shouldImplementEquality() {
        BreakEnded event1 = new BreakEnded(1);
        BreakEnded event2 = new BreakEnded(1);

        assertThat(event1).isEqualTo(event2);
    }

    // ColorUpCompleted tests

    @Test
    void colorUpCompleted_shouldCreateWithCorrectValues() {
        ColorUpCompleted event = new ColorUpCompleted(2);

        assertThat(event.tableId()).isEqualTo(2);
    }

    @Test
    void colorUpCompleted_shouldImplementEquality() {
        ColorUpCompleted event1 = new ColorUpCompleted(2);
        ColorUpCompleted event2 = new ColorUpCompleted(2);

        assertThat(event1).isEqualTo(event2);
    }

    // CurrentPlayerChanged tests

    @Test
    void currentPlayerChanged_shouldCreateWithCorrectValues() {
        CurrentPlayerChanged event = new CurrentPlayerChanged(1, 505);

        assertThat(event.tableId()).isEqualTo(1);
        assertThat(event.playerId()).isEqualTo(505);
    }

    @Test
    void currentPlayerChanged_shouldImplementEquality() {
        CurrentPlayerChanged event1 = new CurrentPlayerChanged(1, 505);
        CurrentPlayerChanged event2 = new CurrentPlayerChanged(1, 505);

        assertThat(event1).isEqualTo(event2);
    }

    // PlayerRebuy tests

    @Test
    void playerRebuy_shouldCreateWithCorrectValues() {
        PlayerRebuy event = new PlayerRebuy(1, 101, 1000);

        assertThat(event.tableId()).isEqualTo(1);
        assertThat(event.playerId()).isEqualTo(101);
        assertThat(event.amount()).isEqualTo(1000);
    }

    @Test
    void playerRebuy_shouldImplementEquality() {
        PlayerRebuy event1 = new PlayerRebuy(1, 101, 1000);
        PlayerRebuy event2 = new PlayerRebuy(1, 101, 1000);
        PlayerRebuy event3 = new PlayerRebuy(1, 101, 2000);

        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
    }

    // PlayerAddon tests

    @Test
    void playerAddon_shouldCreateWithCorrectValues() {
        PlayerAddon event = new PlayerAddon(1, 202, 500);

        assertThat(event.tableId()).isEqualTo(1);
        assertThat(event.playerId()).isEqualTo(202);
        assertThat(event.amount()).isEqualTo(500);
    }

    @Test
    void playerAddon_shouldImplementEquality() {
        PlayerAddon event1 = new PlayerAddon(1, 202, 500);
        PlayerAddon event2 = new PlayerAddon(1, 202, 500);

        assertThat(event1).isEqualTo(event2);
    }

    // ObserverAdded tests

    @Test
    void observerAdded_shouldCreateWithCorrectValues() {
        ObserverAdded event = new ObserverAdded(1, 707);

        assertThat(event.tableId()).isEqualTo(1);
        assertThat(event.observerId()).isEqualTo(707);
    }

    @Test
    void observerAdded_shouldImplementEquality() {
        ObserverAdded event1 = new ObserverAdded(1, 707);
        ObserverAdded event2 = new ObserverAdded(1, 707);

        assertThat(event1).isEqualTo(event2);
    }

    // ObserverRemoved tests

    @Test
    void observerRemoved_shouldCreateWithCorrectValues() {
        ObserverRemoved event = new ObserverRemoved(1, 808);

        assertThat(event.tableId()).isEqualTo(1);
        assertThat(event.observerId()).isEqualTo(808);
    }

    @Test
    void observerRemoved_shouldImplementEquality() {
        ObserverRemoved event1 = new ObserverRemoved(1, 808);
        ObserverRemoved event2 = new ObserverRemoved(1, 808);

        assertThat(event1).isEqualTo(event2);
    }

    // CleaningDone tests

    @Test
    void cleaningDone_shouldCreateWithCorrectValues() {
        CleaningDone event = new CleaningDone(5);

        assertThat(event.tableId()).isEqualTo(5);
    }

    @Test
    void cleaningDone_shouldImplementEquality() {
        CleaningDone event1 = new CleaningDone(5);
        CleaningDone event2 = new CleaningDone(5);

        assertThat(event1).isEqualTo(event2);
    }

    // toString tests for a few events to verify record behavior

    @Test
    void events_shouldHaveReadableToString() {
        PlayerAdded event1 = new PlayerAdded(1, 101, 3);
        LevelChanged event2 = new LevelChanged(1, 5);

        assertThat(event1.toString()).contains("PlayerAdded").contains("tableId=1").contains("playerId=101")
                .contains("seat=3");
        assertThat(event2.toString()).contains("LevelChanged").contains("tableId=1").contains("newLevel=5");
    }
}
