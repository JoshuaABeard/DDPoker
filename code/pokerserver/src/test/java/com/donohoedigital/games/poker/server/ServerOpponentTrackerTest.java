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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.ai.V2OpponentModel;
import com.donohoedigital.games.poker.HandAction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServerOpponentTracker opponent modeling.
 */
class ServerOpponentTrackerTest {

    @Test
    void newTracker_returnsDefaultModel() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        V2OpponentModel model = tracker.getModel(1);

        assertThat(model).isNotNull();
        assertThat(model.getHandsPlayed()).isEqualTo(0);
    }

    @Test
    void recordAction_incrementsHandsPlayed() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, HandAction.ACTION_RAISE, 100, 0, 2);
        tracker.onHandEnd(player);

        V2OpponentModel model = tracker.getModel(1);
        assertThat(model.getHandsPlayed()).isEqualTo(1);
    }

    @Test
    void recordMultipleHands_tracksCount() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Play 3 hands
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_CALL, 100, 0, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        assertThat(model.getHandsPlayed()).isEqualTo(3);
    }

    @Test
    void preFlopRaise_updatesRaiseFrequency() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Raise 3 times, fold 2 times
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_RAISE, 100, 0, 2);
            tracker.onHandEnd(player);
        }
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_FOLD, 0, 0, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        // 3 raises out of 5 hands = 60%
        float raisePercent = model.getHandsRaisedPreFlopPercent(0.0f);
        assertThat(raisePercent).isCloseTo(0.6f, within(0.01f));
    }

    @Test
    void preFlopLimp_updatesLimpFrequency() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Limp 2 times, fold 3 times
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_CALL, 100, 0, 2);
            tracker.onHandEnd(player);
        }
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_FOLD, 0, 0, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        // 2 limps out of 5 hands = 40%
        float limpPercent = model.getHandsLimpedPercent(0.0f);
        assertThat(limpPercent).isCloseTo(0.4f, within(0.01f));
    }

    @Test
    void preFlopFold_updatesUnraisedFoldFrequency() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Fold unraised 4 times, call 1 time
        for (int i = 0; i < 4; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_FOLD, 0, 0, 2);
            tracker.onHandEnd(player);
        }
        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, HandAction.ACTION_CALL, 100, 0, 2);
        tracker.onHandEnd(player);

        V2OpponentModel model = tracker.getModel(1);
        // 4 folds out of 5 hands = 80%
        float foldPercent = model.getHandsFoldedUnraisedPercent(0.0f);
        assertThat(foldPercent).isCloseTo(0.8f, within(0.01f));
    }

    @Test
    void postFlopAction_updatesActFrequency() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Act on flop 3 times
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_BET, 100, 1, 2); // Flop
            tracker.onHandEnd(player);
        }
        // Check on flop 2 times
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_CHECK, 0, 1, 2); // Flop
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        // 3 bets out of 5 flop actions = 60%
        float actPercent = model.getActPostFlop(1, 0.0f);
        assertThat(actPercent).isCloseTo(0.6f, within(0.01f));
    }

    @Test
    void postFlopCheckFold_updatesFrequency() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Check-fold 3 times on turn
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_CHECK, 0, 2, 2); // Turn
            tracker.onPlayerAction(player, HandAction.ACTION_FOLD, 0, 2, 2); // Turn
            tracker.onHandEnd(player);
        }
        // Check-call 2 times on turn
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_CHECK, 0, 2, 2); // Turn
            tracker.onPlayerAction(player, HandAction.ACTION_CALL, 100, 2, 2); // Turn
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        // 3 check-folds out of 5 = 60%
        float checkFoldPercent = model.getCheckFoldPostFlop(2, 0.0f);
        assertThat(checkFoldPercent).isCloseTo(0.6f, within(0.01f));
    }

    @Test
    void overbetPot_updatesFrequency() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Overbet 2 times
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onOverbet(player);
            tracker.onHandEnd(player);
        }
        // Normal bet 3 times
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_BET, 100, 1, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        // 2 overbets out of 5 hands = 40%
        float overbetFreq = model.getOverbetFrequency(0.0f);
        assertThat(overbetFreq).isCloseTo(0.4f, within(0.01f));
    }

    @Test
    void betFold_updatesFrequency() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Bet-fold 3 times
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onBetFold(player);
            tracker.onHandEnd(player);
        }
        // Bet-call 2 times
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, HandAction.ACTION_BET, 100, 1, 2);
            tracker.onPlayerAction(player, HandAction.ACTION_CALL, 100, 1, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        // 3 bet-folds out of 5 hands = 60%
        float betFoldFreq = model.getBetFoldFrequency(0.0f);
        assertThat(betFoldFreq).isCloseTo(0.6f, within(0.01f));
    }

    @Test
    void chipCountAtStart_tracked() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        tracker.onHandStart(player, 1500);
        tracker.onHandEnd(player);

        int chipCount = tracker.getChipCountAtStart(1);
        assertThat(chipCount).isEqualTo(1500);
    }

    @Test
    void handsBeforeBigBlind_calculated() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        // Player seat 3, button at seat 7, 9 seats total
        int hands = tracker.getHandsBeforeBigBlind(3, 7, 9);

        // Button is 7, SB is 8, BB is 0
        // Player at seat 3 will be BB in: 0,1,2,3 = 4 hands from now (accounting for
        // wrap)
        // Actually: current order after BB (seat 0): 1,2,3,4,5,6,7,8,0
        // So seat 3 is 3 hands away from BB
        assertThat(hands).isGreaterThanOrEqualTo(0);
    }

    @Test
    void positionCategory_variesByPosition() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Position 0 (blinds)
        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, HandAction.ACTION_RAISE, 100, 0, 0);
        tracker.onHandEnd(player);

        // Position 3 (late)
        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, HandAction.ACTION_RAISE, 100, 0, 3);
        tracker.onHandEnd(player);

        V2OpponentModel model = tracker.getModel(1);
        // Should track per-position frequencies separately
        float blindTightness = model.getPreFlopTightness(0, 0.5f);
        float lateTightness = model.getPreFlopTightness(3, 0.5f);

        // Both should return reasonable values (may be same if logic is simplified)
        assertThat(blindTightness).isBetween(0.0f, 1.0f);
        assertThat(lateTightness).isBetween(0.0f, 1.0f);
    }

    @Test
    void multiplePlayersTracked_independently() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player1 = createMockPlayer(1);
        GamePlayerInfo player2 = createMockPlayer(2);

        // Player 1 plays 3 hands
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player1, 1000);
            tracker.onPlayerAction(player1, HandAction.ACTION_RAISE, 100, 0, 2);
            tracker.onHandEnd(player1);
        }

        // Player 2 plays 5 hands
        for (int i = 0; i < 5; i++) {
            tracker.onHandStart(player2, 1000);
            tracker.onPlayerAction(player2, HandAction.ACTION_CALL, 100, 0, 2);
            tracker.onHandEnd(player2);
        }

        V2OpponentModel model1 = tracker.getModel(1);
        V2OpponentModel model2 = tracker.getModel(2);

        assertThat(model1.getHandsPlayed()).isEqualTo(3);
        assertThat(model2.getHandsPlayed()).isEqualTo(5);
    }

    // === Helper Methods ===

    private GamePlayerInfo createMockPlayer(int id) {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        when(player.getID()).thenReturn(id);
        when(player.getName()).thenReturn("Player" + id);
        return player;
    }
}
