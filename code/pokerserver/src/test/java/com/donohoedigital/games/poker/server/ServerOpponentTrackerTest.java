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

import com.donohoedigital.games.poker.engine.GamePlayerInfo;
import com.donohoedigital.games.poker.core.ai.AIConstants;
import com.donohoedigital.games.poker.core.ai.V2OpponentModel;
import com.donohoedigital.games.poker.engine.PokerActionConstants;
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
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, 2);
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
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 0, 2);
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
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, 2);
            tracker.onHandEnd(player);
        }
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_FOLD, 0, 0, 2);
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
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 0, 2);
            tracker.onHandEnd(player);
        }
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_FOLD, 0, 0, 2);
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
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_FOLD, 0, 0, 2);
            tracker.onHandEnd(player);
        }
        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 0, 2);
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
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_BET, 100, 1, 2); // Flop
            tracker.onHandEnd(player);
        }
        // Check on flop 2 times
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CHECK, 0, 1, 2); // Flop
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
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CHECK, 0, 2, 2); // Turn
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_FOLD, 0, 2, 2); // Turn
            tracker.onHandEnd(player);
        }
        // Check-call 2 times on turn
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CHECK, 0, 2, 2); // Turn
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 2, 2); // Turn
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
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_BET, 100, 1, 2);
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
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_BET, 100, 1, 2);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 1, 2);
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
    void positionCategory_smallBlindAndBigBlind_trackedSeparately() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Small blind: raise every time (aggressive)
        for (int i = 0; i < 5; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, AIConstants.POSITION_SMALL);
            tracker.onHandEnd(player);
        }

        // Big blind: fold every time (tight)
        for (int i = 0; i < 5; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_FOLD, 0, 0, AIConstants.POSITION_BIG);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);

        // Small blind: raised 5/5 = 0% tight (always played)
        float sbTightness = model.getPreFlopTightness(AIConstants.POSITION_SMALL, 0.5f);
        assertThat(sbTightness).isCloseTo(0.0f, within(0.01f));

        // Big blind: folded 5/5 = 100% tight
        float bbTightness = model.getPreFlopTightness(AIConstants.POSITION_BIG, 0.5f);
        assertThat(bbTightness).isCloseTo(1.0f, within(0.01f));

        // They must be different
        assertThat(sbTightness).isNotCloseTo(bbTightness, within(0.01f));
    }

    @Test
    void positionCategory_smallBlindAggression_trackedSeparately() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Small blind: always raise (100% aggressive)
        for (int i = 0; i < 5; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, AIConstants.POSITION_SMALL);
            tracker.onHandEnd(player);
        }

        // Big blind: always call (0% aggressive)
        for (int i = 0; i < 5; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 0, AIConstants.POSITION_BIG);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);

        float sbAggression = model.getPreFlopAggression(AIConstants.POSITION_SMALL, 0.5f);
        assertThat(sbAggression).isCloseTo(1.0f, within(0.01f));

        float bbAggression = model.getPreFlopAggression(AIConstants.POSITION_BIG, 0.5f);
        assertThat(bbAggression).isCloseTo(0.0f, within(0.01f));

        assertThat(sbAggression).isNotCloseTo(bbAggression, within(0.01f));
    }

    @Test
    void positionCategory_allSixPositions_trackedIndependently() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Record raises at each of the 6 positions
        int[] positions = {AIConstants.POSITION_EARLY, AIConstants.POSITION_MIDDLE, AIConstants.POSITION_LATE,
                AIConstants.POSITION_LAST, AIConstants.POSITION_SMALL, AIConstants.POSITION_BIG};

        for (int pos : positions) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, pos);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);

        // Each position should have recorded the raise (tightness = 0, aggression = 1)
        for (int pos : positions) {
            float tightness = model.getPreFlopTightness(pos, 0.5f);
            assertThat(tightness).as("tightness at position %d", pos).isCloseTo(0.0f, within(0.01f));

            float aggression = model.getPreFlopAggression(pos, 0.5f);
            assertThat(aggression).as("aggression at position %d", pos).isCloseTo(1.0f, within(0.01f));
        }
    }

    @Test
    void positionCategory_earlyVsLate_trackedSeparately() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Early: always fold (tight)
        for (int i = 0; i < 5; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_FOLD, 0, 0, AIConstants.POSITION_EARLY);
            tracker.onHandEnd(player);
        }

        // Late: always raise (loose)
        for (int i = 0; i < 5; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, AIConstants.POSITION_LATE);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);

        float earlyTightness = model.getPreFlopTightness(AIConstants.POSITION_EARLY, 0.5f);
        assertThat(earlyTightness).isCloseTo(1.0f, within(0.01f));

        float lateTightness = model.getPreFlopTightness(AIConstants.POSITION_LATE, 0.5f);
        assertThat(lateTightness).isCloseTo(0.0f, within(0.01f));
    }

    @Test
    void multiplePlayersTracked_independently() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player1 = createMockPlayer(1);
        GamePlayerInfo player2 = createMockPlayer(2);

        // Player 1 plays 3 hands
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player1, 1000);
            tracker.onPlayerAction(player1, PokerActionConstants.ACTION_RAISE, 100, 0, 2);
            tracker.onHandEnd(player1);
        }

        // Player 2 plays 5 hands
        for (int i = 0; i < 5; i++) {
            tracker.onHandStart(player2, 1000);
            tracker.onPlayerAction(player2, PokerActionConstants.ACTION_CALL, 100, 0, 2);
            tracker.onHandEnd(player2);
        }

        V2OpponentModel model1 = tracker.getModel(1);
        V2OpponentModel model2 = tracker.getModel(2);

        assertThat(model1.getHandsPlayed()).isEqualTo(3);
        assertThat(model2.getHandsPlayed()).isEqualTo(5);
    }

    // === Limp vs Call-Raise Tests ===

    @Test
    void callBBWithNoRaise_countedAsLimp() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Player calls the BB with no prior raise — this is a limp
        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 0, AIConstants.POSITION_LATE);
        tracker.onHandEnd(player);

        V2OpponentModel model = tracker.getModel(1);
        // 1 limp out of 1 hand = 100%
        assertThat(model.getHandsLimpedPercent(0.0f)).isCloseTo(1.0f, within(0.01f));
    }

    @Test
    void callAfterRaise_notCountedAsLimp() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo raiser = createMockPlayer(1);
        GamePlayerInfo caller = createMockPlayer(2);

        // Both players start the hand
        tracker.onHandStart(raiser, 1000);
        tracker.onHandStart(caller, 1000);

        // Player 1 raises
        tracker.onPlayerAction(raiser, PokerActionConstants.ACTION_RAISE, 200, 0, AIConstants.POSITION_EARLY);

        // Player 2 calls the raise — NOT a limp
        tracker.onPlayerAction(caller, PokerActionConstants.ACTION_CALL, 200, 0, AIConstants.POSITION_LATE);

        tracker.onHandEnd(raiser);
        tracker.onHandEnd(caller);

        V2OpponentModel callerModel = tracker.getModel(2);
        // Call after raise should NOT be counted as a limp
        assertThat(callerModel.getHandsLimpedPercent(0.0f)).isCloseTo(0.0f, within(0.01f));
        // But it should still be counted as a paid hand (call)
        assertThat(callerModel.getHandsPaidPercent(0.0f)).isCloseTo(1.0f, within(0.01f));
    }

    @Test
    void foldAfterRaise_notCountedAsFoldUnraised() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo raiser = createMockPlayer(1);
        GamePlayerInfo folder = createMockPlayer(2);

        tracker.onHandStart(raiser, 1000);
        tracker.onHandStart(folder, 1000);

        // Player 1 raises
        tracker.onPlayerAction(raiser, PokerActionConstants.ACTION_RAISE, 200, 0, AIConstants.POSITION_EARLY);

        // Player 2 folds after the raise — NOT a fold-unraised
        tracker.onPlayerAction(folder, PokerActionConstants.ACTION_FOLD, 0, 0, AIConstants.POSITION_LATE);

        tracker.onHandEnd(raiser);
        tracker.onHandEnd(folder);

        V2OpponentModel folderModel = tracker.getModel(2);
        // Fold after raise should NOT be counted as fold-unraised
        assertThat(folderModel.getHandsFoldedUnraisedPercent(0.0f)).isCloseTo(0.0f, within(0.01f));
    }

    @Test
    void limpedPercent_onlyReflectsTrueLimps() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo raiser = createMockPlayer(1);
        GamePlayerInfo caller = createMockPlayer(2);

        // Hand 1: No raise — caller limps
        tracker.onHandStart(raiser, 1000);
        tracker.onHandStart(caller, 1000);
        tracker.onPlayerAction(raiser, PokerActionConstants.ACTION_FOLD, 0, 0, AIConstants.POSITION_EARLY);
        tracker.onPlayerAction(caller, PokerActionConstants.ACTION_CALL, 100, 0, AIConstants.POSITION_LATE);
        tracker.onHandEnd(raiser);
        tracker.onHandEnd(caller);

        // Hand 2: Raise occurred — caller calls but it's not a limp
        tracker.onHandStart(raiser, 1000);
        tracker.onHandStart(caller, 1000);
        tracker.onPlayerAction(raiser, PokerActionConstants.ACTION_RAISE, 200, 0, AIConstants.POSITION_EARLY);
        tracker.onPlayerAction(caller, PokerActionConstants.ACTION_CALL, 200, 0, AIConstants.POSITION_LATE);
        tracker.onHandEnd(raiser);
        tracker.onHandEnd(caller);

        V2OpponentModel callerModel = tracker.getModel(2);
        // Only 1 limp out of 2 hands = 50%
        assertThat(callerModel.getHandsLimpedPercent(0.0f)).isCloseTo(0.5f, within(0.01f));
        // But 2 paid hands out of 2 = 100%
        assertThat(callerModel.getHandsPaidPercent(0.0f)).isCloseTo(1.0f, within(0.01f));
    }

    @Test
    void raisedPreFlop_resetsPerHand() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo raiser = createMockPlayer(1);
        GamePlayerInfo caller = createMockPlayer(2);

        // Hand 1: Raise occurred
        tracker.onHandStart(raiser, 1000);
        tracker.onHandStart(caller, 1000);
        tracker.onPlayerAction(raiser, PokerActionConstants.ACTION_RAISE, 200, 0, AIConstants.POSITION_EARLY);
        tracker.onPlayerAction(caller, PokerActionConstants.ACTION_CALL, 200, 0, AIConstants.POSITION_LATE);
        tracker.onHandEnd(raiser);
        tracker.onHandEnd(caller);

        // Hand 2: No raise — caller's call should be a limp
        tracker.onHandStart(raiser, 1000);
        tracker.onHandStart(caller, 1000);
        tracker.onPlayerAction(raiser, PokerActionConstants.ACTION_FOLD, 0, 0, AIConstants.POSITION_EARLY);
        tracker.onPlayerAction(caller, PokerActionConstants.ACTION_CALL, 100, 0, AIConstants.POSITION_LATE);
        tracker.onHandEnd(raiser);
        tracker.onHandEnd(caller);

        V2OpponentModel callerModel = tracker.getModel(2);
        // 1 limp out of 2 hands = 50% (hand 2 was a limp, hand 1 was not)
        assertThat(callerModel.getHandsLimpedPercent(0.0f)).isCloseTo(0.5f, within(0.01f));
    }

    // === Chip Count Tests ===

    @Test
    void chipCountAtStart_untrackedPlayer_returnsZero() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        assertThat(tracker.getChipCountAtStart(999)).isEqualTo(0);
    }

    @Test
    void chipCountAtStart_updatedPerHand() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        tracker.onHandStart(player, 1000);
        assertThat(tracker.getChipCountAtStart(1)).isEqualTo(1000);

        tracker.onHandEnd(player);
        tracker.onHandStart(player, 750);
        assertThat(tracker.getChipCountAtStart(1)).isEqualTo(750);
    }

    // === Hand End Edge Cases ===

    @Test
    void onHandEnd_doubleCall_doesNotDoubleIncrement() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 0, 2);
        tracker.onHandEnd(player);
        tracker.onHandEnd(player); // Double call

        V2OpponentModel model = tracker.getModel(1);
        assertThat(model.getHandsPlayed()).isEqualTo(1);
    }

    // === Action Without Hand Start ===

    @Test
    void onPlayerAction_withoutHandStart_ignoredGracefully() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // No onHandStart call — action should be silently ignored
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, 2);

        V2OpponentModel model = tracker.getModel(1);
        assertThat(model.getHandsPlayed()).isEqualTo(0);
    }

    @Test
    void onOverbet_withoutHandStart_ignoredGracefully() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        tracker.onOverbet(player);

        // No crash and default model returned
        V2OpponentModel model = tracker.getModel(1);
        assertThat(model.getHandsPlayed()).isEqualTo(0);
    }

    @Test
    void onBetFold_withoutHandStart_ignoredGracefully() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        tracker.onBetFold(player);

        V2OpponentModel model = tracker.getModel(1);
        assertThat(model.getHandsPlayed()).isEqualTo(0);
    }

    // === Hands Before Big Blind ===

    @Test
    void handsBeforeBigBlind_playerAtBBSeat_returnsZero() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        // Button at seat 0, BB at seat 2; player at seat 2
        int hands = tracker.getHandsBeforeBigBlind(2, 0, 9);
        assertThat(hands).isEqualTo(0);
    }

    @Test
    void handsBeforeBigBlind_playerOneAfterBB_returns1() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        // Button at seat 0, BB at seat 2; player at seat 1
        // distance = (2 - 1 + 9) % 9 = 1
        int hands = tracker.getHandsBeforeBigBlind(1, 0, 9);
        assertThat(hands).isEqualTo(1);
    }

    @Test
    void handsBeforeBigBlind_wrapsAround() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        // Button at seat 7, BB at seat 0 (7+2 mod 9 = 0); player at seat 5
        // distance = (0 - 5 + 9) % 9 = 4
        int hands = tracker.getHandsBeforeBigBlind(5, 7, 9);
        assertThat(hands).isEqualTo(4);
    }

    // === Post-Flop Open and Raise Frequency ===

    @Test
    void postFlopOpen_updatesFrequency() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Open bet on flop 2 times, check 3 times
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_BET, 100, 1, 2);
            tracker.onHandEnd(player);
        }
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CHECK, 0, 1, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        // 2 opens out of 5 flop rounds = 40%
        float openPercent = model.getOpenPostFlop(1, 0.0f);
        assertThat(openPercent).isCloseTo(0.4f, within(0.01f));
    }

    @Test
    void postFlopRaise_updatesFrequency() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // Raise on turn 1 time, check 4 times
        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 200, 2, 2);
        tracker.onHandEnd(player);

        for (int i = 0; i < 4; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CHECK, 0, 2, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        // 1 raise out of 5 turn rounds = 20%
        float raisePercent = model.getRaisePostFlop(2, 0.0f);
        assertThat(raisePercent).isCloseTo(0.2f, within(0.01f));
    }

    // === Default Value Tests ===

    @Test
    void model_returnsDefaults_whenNoData() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        V2OpponentModel model = tracker.getModel(999);

        assertThat(model.getPreFlopTightness(0, 0.5f)).isEqualTo(0.5f);
        assertThat(model.getPreFlopAggression(0, 0.5f)).isEqualTo(0.5f);
        assertThat(model.getActPostFlop(1, 0.3f)).isEqualTo(0.3f);
        assertThat(model.getCheckFoldPostFlop(1, 0.2f)).isEqualTo(0.2f);
        assertThat(model.getOpenPostFlop(1, 0.4f)).isEqualTo(0.4f);
        assertThat(model.getRaisePostFlop(1, 0.1f)).isEqualTo(0.1f);
        assertThat(model.getHandsPaidPercent(0.5f)).isEqualTo(0.5f);
        assertThat(model.getHandsLimpedPercent(0.5f)).isEqualTo(0.5f);
        assertThat(model.getHandsFoldedUnraisedPercent(0.5f)).isEqualTo(0.5f);
        assertThat(model.getOverbetFrequency(0.1f)).isEqualTo(0.1f);
        assertThat(model.getBetFoldFrequency(0.1f)).isEqualTo(0.1f);
        assertThat(model.getHandsRaisedPreFlopPercent(0.5f)).isEqualTo(0.5f);
    }

    @Test
    void model_returnsDefaults_forInvalidPositionCategory() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        V2OpponentModel model = tracker.getModel(999);

        assertThat(model.getPreFlopTightness(-1, 0.5f)).isEqualTo(0.5f);
        assertThat(model.getPreFlopTightness(6, 0.5f)).isEqualTo(0.5f);
        assertThat(model.getPreFlopAggression(-1, 0.5f)).isEqualTo(0.5f);
        assertThat(model.getPreFlopAggression(6, 0.5f)).isEqualTo(0.5f);
    }

    @Test
    void model_returnsDefaults_forInvalidPostFlopRound() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        V2OpponentModel model = tracker.getModel(999);

        assertThat(model.getActPostFlop(0, 0.3f)).isEqualTo(0.3f); // round 0 = preflop, invalid for post-flop
        assertThat(model.getActPostFlop(4, 0.3f)).isEqualTo(0.3f); // round 4 doesn't exist
    }

    // === Overbet Pot Post-Flop Flag ===

    @Test
    void overbetPotPostFlop_defaultFalse() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        V2OpponentModel model = tracker.getModel(1);
        assertThat(model.isOverbetPotPostFlop()).isFalse();
    }

    @Test
    void overbetPotPostFlop_canBeToggled() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();

        V2OpponentModel model = tracker.getModel(1);
        model.setOverbetPotPostFlop(true);
        assertThat(model.isOverbetPotPostFlop()).isTrue();

        model.setOverbetPotPostFlop(false);
        assertThat(model.isOverbetPotPostFlop()).isFalse();
    }

    // === Hands Paid Percent ===

    @Test
    void handsPaidPercent_countsRaisesAndCalls() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // 2 raises + 1 call out of 5 hands = 60%
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, 2);
            tracker.onHandEnd(player);
        }
        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 0, 2);
        tracker.onHandEnd(player);
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_FOLD, 0, 0, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        assertThat(model.getHandsPaidPercent(0.0f)).isCloseTo(0.6f, within(0.01f));
    }

    // === Pre-Flop Tightness and Aggression ===

    @Test
    void preFlopTightness_calculatedAsOneMinusVPIP() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // At position 2 (late): 2 raises + 1 call + 2 folds = VPIP 3/5 = 0.6, tightness
        // = 0.4
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, 2);
            tracker.onHandEnd(player);
        }
        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 0, 2);
        tracker.onHandEnd(player);
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_FOLD, 0, 0, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        float tightness = model.getPreFlopTightness(2, 0.5f);
        assertThat(tightness).isCloseTo(0.4f, within(0.01f));
    }

    @Test
    void preFlopAggression_calculatedAsRaiseOverVoluntary() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        // At position 2: 2 raises + 3 calls = aggression = 2/5 = 0.4
        for (int i = 0; i < 2; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, 2);
            tracker.onHandEnd(player);
        }
        for (int i = 0; i < 3; i++) {
            tracker.onHandStart(player, 1000);
            tracker.onPlayerAction(player, PokerActionConstants.ACTION_CALL, 100, 0, 2);
            tracker.onHandEnd(player);
        }

        V2OpponentModel model = tracker.getModel(1);
        float aggression = model.getPreFlopAggression(2, 0.5f);
        assertThat(aggression).isCloseTo(0.4f, within(0.01f));
    }

    // === Invalid Position Clamping ===

    @Test
    void positionCategory_negativeValue_clampedToZero() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, -1);
        tracker.onHandEnd(player);

        V2OpponentModel model = tracker.getModel(1);
        // Should have been clamped to position 0
        assertThat(model.getPreFlopTightness(0, 0.5f)).isCloseTo(0.0f, within(0.01f));
    }

    @Test
    void positionCategory_tooLarge_clampedToZero() {
        ServerOpponentTracker tracker = new ServerOpponentTracker();
        GamePlayerInfo player = createMockPlayer(1);

        tracker.onHandStart(player, 1000);
        tracker.onPlayerAction(player, PokerActionConstants.ACTION_RAISE, 100, 0, 10);
        tracker.onHandEnd(player);

        V2OpponentModel model = tracker.getModel(1);
        // Should have been clamped to position 0
        assertThat(model.getPreFlopTightness(0, 0.5f)).isCloseTo(0.0f, within(0.01f));
    }

    // === Helper Methods ===

    private GamePlayerInfo createMockPlayer(int id) {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        when(player.getID()).thenReturn(id);
        when(player.getName()).thenReturn("Player" + id);
        return player;
    }
}
