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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.games.poker.engine.PokerConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProfileValidator - validates and normalizes tournament settings.
 */
public class ProfileValidatorTest {

    // ========== updateNumPlayers() Tests ==========

    @Test
    public void should_ReducePayoutSpots_WhenPlayersDecreaseInSpotsMode() {
        // Given: 10 spots for 100 players, max 5 spots for 20 players
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 10);

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.maxSpotsForPlayers = 5; // Max 5 spots for new player count

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: reduce players to 20
        validator.updateNumPlayers(20);

        // Then: payout spots should be reduced to max
        assertEquals(5, map.getInteger("payoutspots", 0), "Payout spots should be reduced to max");
        assertEquals(20, map.getInteger("numplayers", 0), "Players should be updated");
        assertTrue(callbacks.fixLevelsCalled, "fixLevels should have been called (via fixAll)");
    }

    @Test
    public void should_ReducePayoutPercent_WhenPlayersDecreaseInPercentMode() {
        // Given: 50% payout for 100 players, max 30% for 20 players
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_PERC);
        map.setInteger("payoutperc", 50);

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.maxPercentForPlayers = 30; // Max 30% for new player count

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: update players to 20
        validator.updateNumPlayers(20);

        // Then: payout percent should be reduced to max
        assertEquals(30, map.getInteger("payoutperc", 0), "Payout percent should be reduced to max");
        assertEquals(20, map.getInteger("numplayers", 0), "Players should be updated");
    }

    @Test
    public void should_NotChangeSpots_WhenWithinMax() {
        // Given: 5 spots for 100 players, max 10 spots for new count
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 5);

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.maxSpotsForPlayers = 10; // Max allows current spots

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: update players
        validator.updateNumPlayers(50);

        // Then: payout spots should remain unchanged
        assertEquals(5, map.getInteger("payoutspots", 0), "Payout spots should remain unchanged");
    }

    @Test
    public void should_SwitchToAutoAlloc_WhenSpotsReducedInFixedMode() {
        // Given: fixed allocation mode, spots will be reduced
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 10);
        map.setInteger("alloc", PokerConstants.ALLOC_AMOUNT); // Fixed allocation

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.maxSpotsForPlayers = 5; // Force reduction
        callbacks.allocFixed = true;

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: reduce players
        validator.updateNumPlayers(20);

        // Then: should switch to auto allocation
        assertEquals(PokerConstants.ALLOC_AUTO, map.getInteger("alloc", PokerConstants.ALLOC_AUTO),
                "Should switch to auto allocation");
    }

    @Test
    public void should_CallSetAutoSpots_WhenAllocIsAuto() {
        // Given: auto allocation mode
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 10);
        map.setInteger("alloc", PokerConstants.ALLOC_AUTO);

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.allocAuto = true;

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: update players
        validator.updateNumPlayers(50);

        // Then: setAutoSpots should be called
        assertTrue(callbacks.setAutoSpotsCalled, "setAutoSpots should be called in auto mode");
    }

    @Test
    public void should_NotAdjustSpots_InSatelliteMode() {
        // Given: satellite mode
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SATELLITE);
        map.setInteger("payoutspots", 10);

        TestCallbacks callbacks = new TestCallbacks();

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: update players
        validator.updateNumPlayers(50);

        // Then: spots should remain unchanged (satellite doesn't adjust)
        assertEquals(10, map.getInteger("payoutspots", 0), "Satellite mode should not adjust spots");
    }

    // ========== fixAll() Tests ==========

    @Test
    public void should_CallFixLevels_AndFixAllocs() {
        DMTypedHashMap map = new DMTypedHashMap();
        TestCallbacks callbacks = new TestCallbacks();

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: call fixAll
        validator.fixAll();

        // Then: both methods should be called
        assertTrue(callbacks.fixLevelsCalled, "fixLevels should be called");
        assertTrue(callbacks.getNumSpotsCalled, "fixAllocs should be called implicitly");
    }

    @Test
    public void should_ChangeRebuyExpressionToLTE_WhenChipsAreZero() {
        // Given: rebuy expression is LT (<) and chips are 0
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("rebuyexpr", PokerConstants.REBUY_LT);
        map.setInteger("rebuychips", 0);

        TestCallbacks callbacks = new TestCallbacks();

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: call fixAll
        validator.fixAll();

        // Then: expression should change to LTE (<=)
        assertEquals(PokerConstants.REBUY_LTE, map.getInteger("rebuyexpr", PokerConstants.REBUY_LT),
                "Rebuy expression should change to LTE when chips=0");
    }

    @Test
    public void should_NotChangeRebuyExpression_WhenChipsAreNonZero() {
        // Given: rebuy expression is LT and chips are non-zero
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("rebuyexpr", PokerConstants.REBUY_LT);
        map.setInteger("rebuychips", 500);

        TestCallbacks callbacks = new TestCallbacks();

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: call fixAll
        validator.fixAll();

        // Then: expression should remain LT
        assertEquals(PokerConstants.REBUY_LT, map.getInteger("rebuyexpr", PokerConstants.REBUY_LT),
                "Rebuy expression should remain LT when chips>0");
    }

    @Test
    public void should_NotChangeRebuyExpression_WhenAlreadyLTE() {
        // Given: rebuy expression is already LTE
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("rebuyexpr", PokerConstants.REBUY_LTE);
        map.setInteger("rebuychips", 0);

        TestCallbacks callbacks = new TestCallbacks();

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: call fixAll
        validator.fixAll();

        // Then: expression should remain LTE
        assertEquals(PokerConstants.REBUY_LTE, map.getInteger("rebuyexpr", PokerConstants.REBUY_LT),
                "Rebuy expression should remain LTE");
    }

    // ========== fixAllocs() Tests ==========

    @Test
    public void should_FormatAmountAllocs_InAmountMode() {
        // Given: amount allocation mode with 3 spots
        DMTypedHashMap map = new DMTypedHashMap();

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.numSpots = 3;
        callbacks.allocPercent = false;
        callbacks.spotValues = new double[]{5000, 3000, 2000};

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: call fixAllocs
        validator.fixAllocs();

        // Then: amounts should be formatted as integers
        assertEquals("5000", map.getString("spotamount1"), "Spot 1 should be formatted as integer");
        assertEquals("3000", map.getString("spotamount2"), "Spot 2 should be formatted as integer");
        assertEquals("2000", map.getString("spotamount3"), "Spot 3 should be formatted as integer");
    }

    @Test
    public void should_FormatPercentAllocs_InPercentMode() {
        // Given: percent allocation mode
        DMTypedHashMap map = new DMTypedHashMap();

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.numSpots = 3;
        callbacks.allocPercent = true;
        callbacks.spotValues = new double[]{50.5, 30.25, 19.25};

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: call fixAllocs
        validator.fixAllocs();

        // Then: amounts should be formatted as percentages
        assertEquals("50.5", map.getString("spotamount1"), "Spot 1 should be formatted as percent");
        assertEquals("30.25", map.getString("spotamount2"), "Spot 2 should be formatted as percent");
        assertEquals("19.25", map.getString("spotamount3"), "Spot 3 should be formatted as percent");
    }

    @Test
    public void should_ClearExtraSpots_BeyondNumSpots() {
        // Given: map has 5 spot entries but only 3 are needed
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("spotamount4", "100");
        map.setString("spotamount5", "50");

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.numSpots = 3;
        callbacks.spotValues = new double[]{1000, 500, 250};

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: call fixAllocs
        validator.fixAllocs();

        // Then: extra spots should be cleared
        assertNull(map.getString("spotamount4"), "Spot 4 should be cleared");
        assertNull(map.getString("spotamount5"), "Spot 5 should be cleared");
    }

    @Test
    public void should_OnlyFormatOneSpot_InSatelliteMode() {
        // Given: satellite mode with 10 spots (but should only format 1)
        DMTypedHashMap map = new DMTypedHashMap();

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.numSpots = 10;
        callbacks.allocSatellite = true;
        callbacks.spotValues = new double[]{1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: call fixAllocs
        validator.fixAllocs();

        // Then: only spot 1 should be formatted
        assertNotNull(map.getString("spotamount1"), "Spot 1 should be formatted");
        assertNull(map.getString("spotamount2"), "Spot 2 should not be formatted in satellite mode");
    }

    // ========== Test Helper: Callbacks Implementation ==========

    private static class TestCallbacks implements ProfileValidator.ValidationCallbacks {
        int maxSpotsForPlayers = 10;
        int maxPercentForPlayers = 50;
        boolean allocAuto = false;
        boolean allocFixed = false;
        boolean allocPercent = false;
        boolean allocSatellite = false;
        int numSpots = 0;
        double[] spotValues = new double[0];

        boolean fixLevelsCalled = false;
        boolean setAutoSpotsCalled = false;
        boolean getNumSpotsCalled = false;

        @Override
        public int getMaxPayoutSpots(int numPlayers) {
            return maxSpotsForPlayers;
        }

        @Override
        public int getMaxPayoutPercent(int numPlayers) {
            return maxPercentForPlayers;
        }

        @Override
        public boolean isAllocAuto() {
            return allocAuto;
        }

        @Override
        public boolean isAllocFixed() {
            return allocFixed;
        }

        @Override
        public boolean isAllocPercent() {
            return allocPercent;
        }

        @Override
        public boolean isAllocSatellite() {
            return allocSatellite;
        }

        @Override
        public void setAutoSpots() {
            setAutoSpotsCalled = true;
        }

        @Override
        public void fixLevels() {
            fixLevelsCalled = true;
        }

        @Override
        public int getNumSpots() {
            getNumSpotsCalled = true;
            return numSpots;
        }

        @Override
        public double getSpot(int position) {
            if (position < 1 || position > spotValues.length) {
                return 0;
            }
            return spotValues[position - 1];
        }
    }

    // ========== validateProfile() Tests ==========

    @Test
    public void should_WarnAboutUnreachableLevels_WhenRebuyEndsVeryEarly() {
        // Given: tournament with 20 levels, rebuys until level 2 (10% - below 25%
        // threshold)
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("lastlevel", 20);
        map.setInteger("rebuyuntil", 2);
        map.setBoolean("rebuys", true);

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should warn about unreachable levels
        assertTrue(result.hasWarnings(), "Should warn when rebuy period ends very early");
        assertTrue(result.getWarnings().contains(ValidationWarning.UNREACHABLE_LEVELS),
                "Should contain UNREACHABLE_LEVELS warning");
    }

    @Test
    public void should_NotWarnAboutUnreachableLevels_WhenRebuyPeriodNormal() {
        // Given: tournament with 20 levels, rebuys until level 5 (25% - normal
        // configuration)
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("lastlevel", 20);
        map.setInteger("rebuyuntil", 5);
        map.setBoolean("rebuys", true);

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should not warn for normal rebuy period (>= 25% threshold)
        assertFalse(result.getWarnings().contains(ValidationWarning.UNREACHABLE_LEVELS),
                "Should not warn for normal rebuy period");
    }

    @Test
    public void should_NotWarnAboutUnreachableLevels_WhenRebuyReachesLastLevel() {
        // Given: tournament with 10 levels, rebuys until level 10
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("lastlevel", 10);
        map.setInteger("rebuyuntil", 10);
        map.setBoolean("rebuys", true);

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should not warn about unreachable levels
        assertFalse(result.getWarnings().contains(ValidationWarning.UNREACHABLE_LEVELS),
                "Should not warn when rebuy reaches last level");
    }

    @Test
    public void should_NotWarnAboutUnreachableLevels_WhenRebuysDisabled() {
        // Given: tournament with no rebuys
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("lastlevel", 10);
        map.setBoolean("rebuys", false);

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should not warn (rebuys not enabled)
        assertFalse(result.getWarnings().contains(ValidationWarning.UNREACHABLE_LEVELS),
                "Should not warn when rebuys disabled");
    }

    @Test
    public void should_WarnAboutTooManyPayoutSpots_WhenSpotsExceedPlayers() {
        // Given: 5 payout spots but only 4 players
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 5);
        map.setInteger("numplayers", 4);

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.numSpots = 5;

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should warn about too many spots
        assertTrue(result.getWarnings().contains(ValidationWarning.TOO_MANY_PAYOUT_SPOTS),
                "Should warn about too many payout spots");
    }

    @Test
    public void should_NotWarnAboutPayoutSpots_WhenWithinPlayerCount() {
        // Given: 3 payout spots with 10 players
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 3);
        map.setInteger("numplayers", 10);

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.numSpots = 3;

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should not warn
        assertFalse(result.getWarnings().contains(ValidationWarning.TOO_MANY_PAYOUT_SPOTS),
                "Should not warn when spots within player count");
    }

    @Test
    public void should_WarnAboutShallowDepth_WhenLessThan10BB() {
        // Given: 1500 starting chips, 200 big blind at level 1 (7.5 BB depth)
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("buyinchips", 1500);
        map.setString("big1", "200");

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should warn about shallow depth
        assertTrue(result.getWarnings().contains(ValidationWarning.SHALLOW_STARTING_DEPTH),
                "Should warn about shallow starting depth");
    }

    @Test
    public void should_NotWarnAboutShallowDepth_When10BBOrMore() {
        // Given: 2000 starting chips, 100 big blind at level 1 (20 BB depth)
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("buyinchips", 2000);
        map.setString("big1", "100");

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should not warn
        assertFalse(result.getWarnings().contains(ValidationWarning.SHALLOW_STARTING_DEPTH),
                "Should not warn when depth >= 10BB");
    }

    @Test
    public void should_NotWarnAboutShallowDepth_WhenBigBlindIsZero() {
        // Given: no big blind defined (edge case)
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("buyinchips", 1500);
        map.setString("big1", "0");

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should not warn (can't calculate depth)
        assertFalse(result.getWarnings().contains(ValidationWarning.SHALLOW_STARTING_DEPTH),
                "Should not warn when big blind is zero");
    }

    @Test
    public void should_WarnAboutExcessiveHouseTake_WhenOver20Percent() {
        // Given: 25% house take
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("house", PokerConstants.HOUSE_PERC);
        map.setInteger("houseperc", 25);

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should warn about excessive house take
        assertTrue(result.getWarnings().contains(ValidationWarning.EXCESSIVE_HOUSE_TAKE),
                "Should warn about excessive house take");
    }

    @Test
    public void should_NotWarnAboutHouseTake_When20PercentOrLess() {
        // Given: 15% house take
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("house", PokerConstants.HOUSE_PERC);
        map.setInteger("houseperc", 15);

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should not warn
        assertFalse(result.getWarnings().contains(ValidationWarning.EXCESSIVE_HOUSE_TAKE),
                "Should not warn when house take <= 20%");
    }

    @Test
    public void should_CheckHouseTakeAsPercentOfBuyin_WhenUsingAmountMode() {
        // Given: $100 buyin with $25 house amount (25%)
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("house", PokerConstants.HOUSE_AMOUNT);
        map.setInteger("houseamount", 25);
        map.setInteger("buyin", 100);

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should warn (25% is over threshold)
        assertTrue(result.getWarnings().contains(ValidationWarning.EXCESSIVE_HOUSE_TAKE),
                "Should warn about excessive house amount");
    }

    @Test
    public void should_NotWarnAboutHouseAmount_WhenBelow20Percent() {
        // Given: $100 buyin with $15 house amount (15%)
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("house", PokerConstants.HOUSE_AMOUNT);
        map.setInteger("houseamount", 15);
        map.setInteger("buyin", 100);

        TestCallbacks callbacks = new TestCallbacks();
        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should not warn
        assertFalse(result.getWarnings().contains(ValidationWarning.EXCESSIVE_HOUSE_TAKE),
                "Should not warn when house amount < 20% of buyin");
    }

    @Test
    public void should_ReturnMultipleWarnings_WhenMultipleIssuesExist() {
        // Given: profile with multiple issues
        DMTypedHashMap map = new DMTypedHashMap();
        // Too many payout spots
        map.setInteger("payout", PokerConstants.PAYOUT_SPOTS);
        map.setInteger("payoutspots", 10);
        map.setInteger("numplayers", 5);
        // Excessive house take
        map.setInteger("house", PokerConstants.HOUSE_PERC);
        map.setInteger("houseperc", 25);

        TestCallbacks callbacks = new TestCallbacks();
        callbacks.numSpots = 10;

        ProfileValidator validator = new ProfileValidator(map, callbacks);

        // When: validate profile
        ValidationResult result = validator.validateProfile();

        // Then: should return both warnings
        assertTrue(result.hasWarnings(), "Should have warnings");
        assertEquals(2, result.getWarnings().size(), "Should have 2 warnings");
        assertTrue(result.getWarnings().contains(ValidationWarning.TOO_MANY_PAYOUT_SPOTS),
                "Should warn about payout spots");
        assertTrue(result.getWarnings().contains(ValidationWarning.EXCESSIVE_HOUSE_TAKE),
                "Should warn about house take");
    }
}
