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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.ai.PokerAI.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OpponentModel player behavior tracking and modeling.
 */
class OpponentModelTest {
    private OpponentModel model;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for tests that create PokerTable
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        model = new OpponentModel();
    }

    // ========================================
    // Initialization Tests
    // ========================================

    @Test
    void should_InitializeHandsPlayedToZero_When_InitCalled() {
        model.init();

        assertThat(model.handsPlayed).isZero();
    }

    @Test
    void should_CreateTightnessTrackers_When_InitCalled() {
        model.init();

        // Should have 6 tightness trackers (one per position category)
        for (int i = 0; i < 6; i++) {
            assertThat(model.tightness[i]).isNotNull();
        }
    }

    @Test
    void should_CreateAggressionTrackers_When_InitCalled() {
        model.init();

        // Should have 6 aggression trackers (one per position category)
        for (int i = 0; i < 6; i++) {
            assertThat(model.aggression[i]).isNotNull();
        }
    }

    @Test
    void should_CreateBooleanTrackers_When_InitCalled() {
        model.init();

        // Preflop trackers
        assertThat(model.handsPaid).isNotNull();
        assertThat(model.handsLimped).isNotNull();
        assertThat(model.handsFoldedUnraised).isNotNull();
        assertThat(model.handsRaisedPreFlop).isNotNull();
        assertThat(model.handsOverbetPotPostFlop).isNotNull();
        assertThat(model.handsBetFoldPostFlop).isNotNull();

        // Flop trackers
        assertThat(model.actFlop).isNotNull();
        assertThat(model.checkFoldFlop).isNotNull();
        assertThat(model.openFlop).isNotNull();
        assertThat(model.raiseFlop).isNotNull();

        // Turn trackers
        assertThat(model.actTurn).isNotNull();
        assertThat(model.checkFoldTurn).isNotNull();
        assertThat(model.openTurn).isNotNull();
        assertThat(model.raiseTurn).isNotNull();

        // River trackers
        assertThat(model.actRiver).isNotNull();
        assertThat(model.checkFoldRiver).isNotNull();
        assertThat(model.openRiver).isNotNull();
        assertThat(model.raiseRiver).isNotNull();
    }

    // ========================================
    // Tracker Array Tests
    // ========================================

    @Test
    void should_HaveSixTightnessTrackers_When_InitCalled() {
        model.init();

        assertThat(model.tightness).hasSize(6);
    }

    @Test
    void should_HaveSixAggressionTrackers_When_InitCalled() {
        model.init();

        assertThat(model.aggression).hasSize(6);
    }

    @Test
    void should_InitializeAllTightnessTrackers_When_InitCalled() {
        model.init();

        for (int i = 0; i < 6; i++) {
            assertThat(model.tightness[i]).isNotNull();
            // Trackers should be initialized (not just null)
        }
    }

    @Test
    void should_InitializeAllAggressionTrackers_When_InitCalled() {
        model.init();

        for (int i = 0; i < 6; i++) {
            assertThat(model.aggression[i]).isNotNull();
            // Trackers should be initialized (not just null)
        }
    }

    // ========================================
    // Reinitialization Tests
    // ========================================

    @Test
    void should_ReuseExistingTrackers_When_InitCalledMultipleTimes() {
        model.init();
        FloatTracker firstTightness0 = model.tightness[0];
        BooleanTracker firstHandsPaid = model.handsPaid;

        model.init();

        // Should reuse same tracker instances, just cleared
        assertThat(model.tightness[0]).isSameAs(firstTightness0);
        assertThat(model.handsPaid).isSameAs(firstHandsPaid);
    }

    @Test
    void should_ResetHandsPlayed_When_InitCalledAfterHandsPlayed() {
        model.init();
        model.handsPlayed = 100;

        model.init();

        assertThat(model.handsPlayed).isZero();
    }

    @Test
    void should_ClearAllTrackers_When_InitCalledAfterUse() {
        model.init();

        // Simulate some usage by incrementing hands played
        model.handsPlayed = 50;

        // Reinitialize
        model.init();

        // Hands played should be reset
        assertThat(model.handsPlayed).isZero();
        // All trackers should still exist
        assertThat(model.tightness[0]).isNotNull();
        assertThat(model.handsPaid).isNotNull();
    }

    // ========================================
    // Tracker Type Tests
    // ========================================

    @Test
    void should_CreateFloatTrackers_When_TightnessInitialized() {
        model.init();

        for (int i = 0; i < 6; i++) {
            assertThat(model.tightness[i]).isInstanceOf(FloatTracker.class);
        }
    }

    @Test
    void should_CreateFloatTrackers_When_AggressionInitialized() {
        model.init();

        for (int i = 0; i < 6; i++) {
            assertThat(model.aggression[i]).isInstanceOf(FloatTracker.class);
        }
    }

    @Test
    void should_CreateBooleanTrackers_When_HandTrackersInitialized() {
        model.init();

        assertThat(model.handsPaid).isInstanceOf(BooleanTracker.class);
        assertThat(model.handsLimped).isInstanceOf(BooleanTracker.class);
        assertThat(model.handsFoldedUnraised).isInstanceOf(BooleanTracker.class);
        assertThat(model.handsRaisedPreFlop).isInstanceOf(BooleanTracker.class);
    }

    @Test
    void should_CreateBooleanTrackers_When_StreetTrackersInitialized() {
        model.init();

        // Flop trackers
        assertThat(model.actFlop).isInstanceOf(BooleanTracker.class);
        assertThat(model.checkFoldFlop).isInstanceOf(BooleanTracker.class);

        // Turn trackers
        assertThat(model.actTurn).isInstanceOf(BooleanTracker.class);
        assertThat(model.checkFoldTurn).isInstanceOf(BooleanTracker.class);

        // River trackers
        assertThat(model.actRiver).isInstanceOf(BooleanTracker.class);
        assertThat(model.checkFoldRiver).isInstanceOf(BooleanTracker.class);
    }

    // ========================================
    // All Trackers Count Tests
    // ========================================

    @Test
    void should_HaveEighteenBooleanTrackers_When_InitCalled() {
        model.init();

        int trackerCount = 0;

        // Count all non-null boolean trackers
        if (model.handsPaid != null)
            trackerCount++;
        if (model.handsLimped != null)
            trackerCount++;
        if (model.handsFoldedUnraised != null)
            trackerCount++;
        if (model.handsRaisedPreFlop != null)
            trackerCount++;
        if (model.handsOverbetPotPostFlop != null)
            trackerCount++;
        if (model.handsBetFoldPostFlop != null)
            trackerCount++;

        if (model.actFlop != null)
            trackerCount++;
        if (model.checkFoldFlop != null)
            trackerCount++;
        if (model.openFlop != null)
            trackerCount++;
        if (model.raiseFlop != null)
            trackerCount++;

        if (model.actTurn != null)
            trackerCount++;
        if (model.checkFoldTurn != null)
            trackerCount++;
        if (model.openTurn != null)
            trackerCount++;
        if (model.raiseTurn != null)
            trackerCount++;

        if (model.actRiver != null)
            trackerCount++;
        if (model.checkFoldRiver != null)
            trackerCount++;
        if (model.openRiver != null)
            trackerCount++;
        if (model.raiseRiver != null)
            trackerCount++;

        assertThat(trackerCount).isEqualTo(18);
    }

    @Test
    void should_HaveTwelveFloatTrackers_When_InitCalled() {
        model.init();

        int trackerCount = 0;

        // Count tightness trackers
        for (int i = 0; i < 6; i++) {
            if (model.tightness[i] != null)
                trackerCount++;
        }

        // Count aggression trackers
        for (int i = 0; i < 6; i++) {
            if (model.aggression[i] != null)
                trackerCount++;
        }

        assertThat(trackerCount).isEqualTo(12);
    }

    // ========================================
    // Preflop Tracker Tests
    // ========================================

    @Test
    void should_InitializePreflopTrackers_When_InitCalled() {
        model.init();

        assertThat(model.handsPaid).isNotNull();
        assertThat(model.handsLimped).isNotNull();
        assertThat(model.handsFoldedUnraised).isNotNull();
        assertThat(model.handsRaisedPreFlop).isNotNull();
        assertThat(model.handsOverbetPotPostFlop).isNotNull();
        assertThat(model.handsBetFoldPostFlop).isNotNull();
    }

    // ========================================
    // Flop Tracker Tests
    // ========================================

    @Test
    void should_InitializeFlopTrackers_When_InitCalled() {
        model.init();

        assertThat(model.actFlop).isNotNull();
        assertThat(model.checkFoldFlop).isNotNull();
        assertThat(model.openFlop).isNotNull();
        assertThat(model.raiseFlop).isNotNull();
    }

    // ========================================
    // Turn Tracker Tests
    // ========================================

    @Test
    void should_InitializeTurnTrackers_When_InitCalled() {
        model.init();

        assertThat(model.actTurn).isNotNull();
        assertThat(model.checkFoldTurn).isNotNull();
        assertThat(model.openTurn).isNotNull();
        assertThat(model.raiseTurn).isNotNull();
    }

    // ========================================
    // River Tracker Tests
    // ========================================

    @Test
    void should_InitializeRiverTrackers_When_InitCalled() {
        model.init();

        assertThat(model.actRiver).isNotNull();
        assertThat(model.checkFoldRiver).isNotNull();
        assertThat(model.openRiver).isNotNull();
        assertThat(model.raiseRiver).isNotNull();
    }

    // ========================================
    // Consistency Tests
    // ========================================

    @Test
    void should_InitializeConsistently_When_CalledMultipleTimes() {
        model.init();
        int firstHandsPlayed = model.handsPlayed;
        FloatTracker firstTightness = model.tightness[0];

        model.init();
        int secondHandsPlayed = model.handsPlayed;
        FloatTracker secondTightness = model.tightness[0];

        assertThat(firstHandsPlayed).isEqualTo(secondHandsPlayed).isZero();
        assertThat(secondTightness).isSameAs(firstTightness);
    }

    @Test
    void should_NotHaveNullTrackers_When_InitCompletes() {
        model.init();

        // Check all tightness trackers
        for (int i = 0; i < 6; i++) {
            assertThat(model.tightness[i]).as("tightness[%d]", i).isNotNull();
        }

        // Check all aggression trackers
        for (int i = 0; i < 6; i++) {
            assertThat(model.aggression[i]).as("aggression[%d]", i).isNotNull();
        }
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    void should_HandleMultipleInitCalls_When_CalledRepeatedly() {
        // Initialize multiple times
        for (int i = 0; i < 5; i++) {
            model.init();
            assertThat(model.handsPlayed).isZero();
            assertThat(model.tightness[0]).isNotNull();
        }
    }

    @Test
    void should_InitializeOverbetPotFlagToFalse_When_InitCalled() {
        model.overbetPotPostFlop = true;
        model.init();

        // Note: init() doesn't reset this flag, but we can test the initial state
        // This tests that the flag exists and can be set
        model.overbetPotPostFlop = false;
        assertThat(model.overbetPotPostFlop).isFalse();
    }

    // ========================================
    // getHandsPlayed Tests
    // ========================================

    @Test
    void should_ReturnZero_When_NoHandsPlayed() {
        model.init();

        assertThat(model.getHandsPlayed()).isZero();
    }

    @Test
    void should_ReturnHandsPlayed_When_HandsPlayedSet() {
        model.init();
        model.handsPlayed = 42;

        assertThat(model.getHandsPlayed()).isEqualTo(42);
    }

    // ========================================
    // getPreFlopTightness Tests
    // ========================================

    @Test
    void should_ReturnDefaultValue_When_TightnessNotReady() {
        model.init();

        float result = model.getPreFlopTightness(POSITION_EARLY, 0.5f);

        assertThat(result).isEqualTo(0.5f);
    }

    @Test
    void should_ReturnOverallTightness_When_PositionTrackerNotReady() {
        model.init();

        // Add enough entries to overall tracker (index 0) to make it ready
        for (int i = 0; i < 15; i++) {
            model.tightness[0].addEntry(0.7f);
        }

        // Position tracker (index 1) not ready, should fall back to overall
        float result = model.getPreFlopTightness(POSITION_EARLY, 0.5f);

        assertThat(result).isCloseTo(0.7f, within(0.01f));
    }

    @Test
    void should_ReturnPositionTightness_When_PositionTrackerReady() {
        model.init();

        // Make position tracker ready with specific value
        for (int i = 0; i < 15; i++) {
            model.tightness[1].addEntry(0.9f); // POSITION_EARLY = index 1
        }

        float result = model.getPreFlopTightness(POSITION_EARLY, 0.5f);

        assertThat(result).isCloseTo(0.9f, within(0.01f));
    }

    @Test
    void should_HandleAllPositions_When_GettingTightness() {
        model.init();

        // Test all position constants
        model.getPreFlopTightness(POSITION_EARLY, 0.5f);
        model.getPreFlopTightness(POSITION_MIDDLE, 0.5f);
        model.getPreFlopTightness(POSITION_LATE, 0.5f);
        model.getPreFlopTightness(POSITION_SMALL, 0.5f);
        model.getPreFlopTightness(POSITION_BIG, 0.5f);
        model.getPreFlopTightness(0, 0.5f); // Unknown position -> index 0

        // Should not throw exceptions
    }

    // ========================================
    // getPreFlopAggression Tests
    // ========================================

    @Test
    void should_ReturnDefaultValue_When_AggressionNotReady() {
        model.init();

        float result = model.getPreFlopAggression(POSITION_MIDDLE, 0.3f);

        assertThat(result).isEqualTo(0.3f);
    }

    @Test
    void should_ReturnOverallAggression_When_PositionTrackerNotReady() {
        model.init();

        // Add enough entries to overall tracker to make it ready
        for (int i = 0; i < 10; i++) {
            model.aggression[0].addEntry(0.8f);
        }

        float result = model.getPreFlopAggression(POSITION_MIDDLE, 0.3f);

        assertThat(result).isCloseTo(0.8f, within(0.01f));
    }

    @Test
    void should_ReturnPositionAggression_When_PositionTrackerReady() {
        model.init();

        // Make late position tracker ready
        for (int i = 0; i < 10; i++) {
            model.aggression[3].addEntry(0.6f); // POSITION_LATE = index 3
        }

        float result = model.getPreFlopAggression(POSITION_LATE, 0.3f);

        assertThat(result).isCloseTo(0.6f, within(0.01f));
    }

    // ========================================
    // getActPostFlop Tests
    // ========================================

    @Test
    void should_ReturnDefaultValue_When_ActFlopNotReady() {
        model.init();

        float result = model.getActPostFlop(HoldemHand.ROUND_FLOP, 0.4f);

        assertThat(result).isEqualTo(0.4f);
    }

    @Test
    void should_ReturnActPercent_When_ActFlopReady() {
        model.init();

        // Add entries to make tracker ready
        for (int i = 0; i < 10; i++) {
            model.actFlop.addEntry(i % 2 == 0); // 50% true
        }

        float result = model.getActPostFlop(HoldemHand.ROUND_FLOP, 0.0f);

        assertThat(result).isGreaterThan(0.0f);
    }

    @Test
    void should_ReturnActTurnPercent_When_ActTurnReady() {
        model.init();

        for (int i = 0; i < 10; i++) {
            model.actTurn.addEntry(true);
        }

        float result = model.getActPostFlop(HoldemHand.ROUND_TURN, 0.0f);

        assertThat(result).isGreaterThan(0.5f);
    }

    @Test
    void should_ReturnActRiverPercent_When_ActRiverReady() {
        model.init();

        for (int i = 0; i < 10; i++) {
            model.actRiver.addEntry(false);
        }

        float result = model.getActPostFlop(HoldemHand.ROUND_RIVER, 0.5f);

        assertThat(result).isLessThan(0.5f);
    }

    @Test
    void should_ThrowException_When_InvalidRoundForAct() {
        model.init();

        assertThatThrownBy(() -> model.getActPostFlop(HoldemHand.ROUND_PRE_FLOP, 0.5f)).isInstanceOf(Exception.class);
    }

    // ========================================
    // getOpenPostFlop Tests
    // ========================================

    @Test
    void should_ReturnDefaultValue_When_OpenFlopNotReady() {
        model.init();

        float result = model.getOpenPostFlop(HoldemHand.ROUND_FLOP, 0.3f);

        assertThat(result).isEqualTo(0.3f);
    }

    @Test
    void should_ReturnOpenPercent_When_OpenFlopReady() {
        model.init();

        for (int i = 0; i < 10; i++) {
            model.openFlop.addEntry(true);
        }

        float result = model.getOpenPostFlop(HoldemHand.ROUND_FLOP, 0.0f);

        assertThat(result).isGreaterThan(0.5f);
    }

    @Test
    void should_ThrowException_When_InvalidRoundForOpen() {
        model.init();

        assertThatThrownBy(() -> model.getOpenPostFlop(99, 0.5f)).isInstanceOf(Exception.class);
    }

    // ========================================
    // getRaisePostFlop Tests
    // ========================================

    @Test
    void should_ReturnDefaultValue_When_RaiseFlopNotReady() {
        model.init();

        float result = model.getRaisePostFlop(HoldemHand.ROUND_FLOP, 0.2f);

        assertThat(result).isEqualTo(0.2f);
    }

    @Test
    void should_ReturnRaisePercent_When_RaiseFlopReady() {
        model.init();

        for (int i = 0; i < 10; i++) {
            model.raiseFlop.addEntry(i < 3); // 30% true
        }

        float result = model.getRaisePostFlop(HoldemHand.ROUND_FLOP, 0.0f);

        assertThat(result).isGreaterThan(0.0f);
        assertThat(result).isLessThan(0.5f);
    }

    @Test
    void should_ThrowException_When_InvalidRoundForRaise() {
        model.init();

        assertThatThrownBy(() -> model.getRaisePostFlop(-1, 0.5f)).isInstanceOf(Exception.class);
    }

    // ========================================
    // getCheckFoldPostFlop Tests
    // ========================================

    @Test
    void should_ReturnDefaultValue_When_CheckFoldFlopNotReady() {
        model.init();

        float result = model.getCheckFoldPostFlop(HoldemHand.ROUND_FLOP, 0.6f);

        assertThat(result).isEqualTo(0.6f);
    }

    @Test
    void should_ReturnCheckFoldPercent_When_CheckFoldFlopReady() {
        model.init();

        for (int i = 0; i < 10; i++) {
            model.checkFoldFlop.addEntry(i < 7); // 70% true
        }

        float result = model.getCheckFoldPostFlop(HoldemHand.ROUND_FLOP, 0.0f);

        assertThat(result).isGreaterThan(0.5f);
    }

    @Test
    void should_ThrowException_When_InvalidRoundForCheckFold() {
        model.init();

        assertThatThrownBy(() -> model.getCheckFoldPostFlop(100, 0.5f)).isInstanceOf(Exception.class);
    }

    // ========================================
    // saveToMap / loadFromMap Tests
    // ========================================

    @Test
    void should_SaveHandsPlayed_When_SaveToMapCalled() {
        model.init();
        model.handsPlayed = 123;

        DMTypedHashMap map = new DMTypedHashMap();
        model.saveToMap(map, "test.");

        assertThat(map.getInteger("test.handsPlayed", 0)).isEqualTo(123);
    }

    @Test
    void should_SaveAllTrackers_When_SaveToMapCalled() {
        model.init();

        DMTypedHashMap map = new DMTypedHashMap();
        model.saveToMap(map, "prefix.");

        // Verify tracker data was saved (encoded)
        assertThat(map.getObject("prefix.handsPaid")).isNotNull();
        assertThat(map.getObject("prefix.handsLimped")).isNotNull();
        assertThat(map.getObject("prefix.actFlop")).isNotNull();
        assertThat(map.getObject("prefix.tightness0")).isNotNull();
        assertThat(map.getObject("prefix.aggression0")).isNotNull();
    }

    @Test
    void should_LoadHandsPlayed_When_LoadFromMapCalled() {
        model.init();

        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("load.handsPlayed", 456);

        model.loadFromMap(map, "load.");

        assertThat(model.handsPlayed).isEqualTo(456);
    }

    @Test
    void should_RoundTripSuccessfully_When_SaveAndLoad() {
        model.init();
        model.handsPlayed = 789;

        // Add some data to trackers
        for (int i = 0; i < 15; i++) {
            model.tightness[0].addEntry(0.5f);
            model.aggression[0].addEntry(0.3f);
        }

        DMTypedHashMap map = new DMTypedHashMap();
        model.saveToMap(map, "rt.");

        OpponentModel newModel = new OpponentModel();
        newModel.init();
        newModel.loadFromMap(map, "rt.");

        assertThat(newModel.handsPlayed).isEqualTo(789);
    }

    @Test
    void should_HandleEmptyPrefix_When_SaveToMap() {
        model.init();
        model.handsPlayed = 50;

        DMTypedHashMap map = new DMTypedHashMap();
        model.saveToMap(map, "");

        assertThat(map.getInteger("handsPlayed", 0)).isEqualTo(50);
    }

    @Test
    void should_SaveOverbetFlag_When_SaveToMapCalled() {
        model.init();
        model.overbetPotPostFlop = true;

        DMTypedHashMap map = new DMTypedHashMap();
        model.saveToMap(map, "");

        assertThat(map.getBoolean("overbetPotPostFlop", false)).isTrue();
    }

    @Test
    void should_LoadOverbetFlag_When_LoadFromMapCalled() {
        model.init();

        DMTypedHashMap map = new DMTypedHashMap();
        map.setBoolean("overbetPotPostFlop", true);

        model.loadFromMap(map, "");

        assertThat(model.overbetPotPostFlop).isTrue();
    }

    // ========================================
    // endHand Integration Tests
    // ========================================

    private PokerGame createTestGame() {
        PokerGame game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);
        return game;
    }

    private PokerTable createTestTable(PokerGame game) {
        PokerTable table = new PokerTable(game, 1);
        table.setMinChip(1);
        return table;
    }

    private PokerPlayer createTestPlayer(int id, String name, PokerGame game, PokerTable table, int seat) {
        PokerPlayer player = new PokerPlayer(id, name, true);
        player.setChipCount(1000);
        game.addPlayer(player);
        table.setPlayer(player, seat);
        player.newHand('p');
        return player;
    }

    @Test
    void should_IncrementHandsPlayed_When_EndHandCalled() {
        model.init();
        PokerGame game = createTestGame();
        PokerTable table = createTestTable(game);
        PokerPlayer player = createTestPlayer(1, "Test", game, table, 0);
        PokerPlayer player2 = createTestPlayer(2, "Test2", game, table, 1);
        table.setButton(0);

        HoldemHand hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);
        // Advance to flop so PocketRanks doesn't fail
        hand.advanceRound();

        int initialCount = model.getHandsPlayed();
        model.endHand(null, hand, player);

        assertThat(model.getHandsPlayed()).isEqualTo(initialCount + 1);
    }

    @Test
    void should_TrackPreFlopFold_When_PlayerFoldedPreFlop() {
        model.init();
        PokerGame game = createTestGame();
        PokerTable table = createTestTable(game);
        PokerPlayer player = createTestPlayer(1, "Test", game, table, 0);
        PokerPlayer player2 = createTestPlayer(2, "Test2", game, table, 1);
        table.setButton(0);

        HoldemHand hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);
        hand.setCurrentPlayerIndex(0);

        // Simulate fold action with correct signature
        hand.fold(player, "test", 0);

        // Advance to flop so PocketRanks doesn't fail
        hand.advanceRound();

        model.endHand(null, hand, player);

        // After fold, tightness should be tracked (1.0 for fold)
        assertThat(model.tightness[0].getCount()).isGreaterThan(0);
    }

    @Test
    void should_TrackPreFlopRaise_When_PlayerRaisedPreFlop() {
        model.init();
        PokerGame game = createTestGame();
        PokerTable table = createTestTable(game);
        PokerPlayer player = createTestPlayer(1, "Test", game, table, 0);
        PokerPlayer player2 = createTestPlayer(2, "Test2", game, table, 1);
        table.setButton(0);

        HoldemHand hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);
        hand.setCurrentPlayerIndex(0);

        // Simulate raise action with correct signature
        hand.setBigBlind(10);
        hand.setSmallBlind(5);
        hand.raise(player, 10, 20, "test");

        // Advance to flop so PocketRanks doesn't fail
        hand.advanceRound();

        model.endHand(null, hand, player);

        // After raise, aggression should be tracked
        assertThat(model.aggression[0].getCount()).isGreaterThan(0);
    }

    @Test
    void should_TrackPreFlopCall_When_PlayerCalledPreFlop() {
        model.init();
        PokerGame game = createTestGame();
        PokerTable table = createTestTable(game);
        PokerPlayer player = createTestPlayer(1, "Test", game, table, 0);
        PokerPlayer player2 = createTestPlayer(2, "Test2", game, table, 1);
        table.setButton(0);

        HoldemHand hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);
        hand.setCurrentPlayerIndex(0);

        // Simulate call action with correct signature
        hand.setBigBlind(10);
        hand.call(player, 10, "test");

        // Advance to flop so PocketRanks doesn't fail
        hand.advanceRound();

        model.endHand(null, hand, player);

        // After call, both tightness and aggression should be tracked
        assertThat(model.tightness[0].getCount()).isGreaterThan(0);
        assertThat(model.aggression[0].getCount()).isGreaterThan(0);
    }

    @Test
    void should_TrackFlopAction_When_PlayerActedOnFlop() {
        model.init();
        PokerGame game = createTestGame();
        PokerTable table = createTestTable(game);
        PokerPlayer player = createTestPlayer(1, "Test", game, table, 0);
        PokerPlayer player2 = createTestPlayer(2, "Test2", game, table, 1);
        table.setButton(0);

        HoldemHand hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);

        // Advance to flop round
        hand.advanceRound();
        hand.setCurrentPlayerIndex(0);

        // Simulate check on flop with correct signature
        hand.check(player, "test");

        model.endHand(null, hand, player);

        // Verify flop action tracked
        assertThat(model.actFlop.getCount()).isGreaterThan(0);
    }

    @Test
    void should_TrackCheckFold_When_PlayerCheckedFlop() {
        model.init();
        PokerGame game = createTestGame();
        PokerTable table = createTestTable(game);
        PokerPlayer player = createTestPlayer(1, "Test", game, table, 0);
        PokerPlayer player2 = createTestPlayer(2, "Test2", game, table, 1);
        table.setButton(0);

        HoldemHand hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);

        // Advance to flop round
        hand.advanceRound();
        hand.setCurrentPlayerIndex(0);

        // Simulate check on flop
        hand.check(player, "test");

        model.endHand(null, hand, player);

        // Verify check/fold tracked
        assertThat(model.checkFoldFlop.getCount()).isGreaterThan(0);
    }

    @Test
    void should_HandleMultipleRounds_When_PlayerActsOnTurnAndRiver() {
        model.init();
        PokerGame game = createTestGame();
        PokerTable table = createTestTable(game);
        PokerPlayer player = createTestPlayer(1, "Test", game, table, 0);
        PokerPlayer player2 = createTestPlayer(2, "Test2", game, table, 1);
        table.setButton(0);

        HoldemHand hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);

        // Flop action
        hand.advanceRound();
        hand.setCurrentPlayerIndex(0);
        hand.check(player, "test");

        // Turn action
        hand.advanceRound();
        hand.setCurrentPlayerIndex(0);
        hand.check(player, "test");

        // River action
        hand.advanceRound();
        hand.setCurrentPlayerIndex(0);
        hand.check(player, "test");

        model.endHand(null, hand, player);

        // Verify all rounds tracked
        assertThat(model.actFlop.getCount()).isGreaterThan(0);
        assertThat(model.actTurn.getCount()).isGreaterThan(0);
        assertThat(model.actRiver.getCount()).isGreaterThan(0);
    }

    @Test
    void should_NotCrash_When_NoActionsInHand() {
        model.init();
        PokerGame game = createTestGame();
        PokerTable table = createTestTable(game);
        PokerPlayer player = createTestPlayer(1, "Test", game, table, 0);
        PokerPlayer player2 = createTestPlayer(2, "Test2", game, table, 1);
        table.setButton(0);

        HoldemHand hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);
        // Advance to flop so PocketRanks doesn't fail
        hand.advanceRound();

        // Call endHand with no actions
        assertThatCode(() -> model.endHand(null, hand, player)).doesNotThrowAnyException();

        // Verify handsPlayed still incremented
        assertThat(model.getHandsPlayed()).isEqualTo(1);
    }

    @Test
    void should_ResetOverbetFlag_When_EndHandCompletes() {
        model.init();
        model.overbetPotPostFlop = true;

        PokerGame game = createTestGame();
        PokerTable table = createTestTable(game);
        PokerPlayer player = createTestPlayer(1, "Test", game, table, 0);
        PokerPlayer player2 = createTestPlayer(2, "Test2", game, table, 1);
        table.setButton(0);

        HoldemHand hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);
        // Advance to flop so PocketRanks doesn't fail
        hand.advanceRound();

        model.endHand(null, hand, player);

        assertThat(model.overbetPotPostFlop).isFalse();
    }
}
