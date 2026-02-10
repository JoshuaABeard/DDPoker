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
package com.donohoedigital.games.poker.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for BooleanTracker - tracks boolean values with percentage calculations.
 */
class BooleanTrackerTest {

    private BooleanTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new BooleanTracker(5, 2);
    }

    // =================================================================
    // Constructor and Initialization Tests
    // =================================================================

    @Test
    void should_CreateTracker_When_ValidParametersProvided() {
        BooleanTracker newTracker = new BooleanTracker(10, 3);

        assertThat(newTracker).isNotNull();
        assertThat(newTracker.getCount()).isEqualTo(10);
        assertThat(newTracker.isFull()).isFalse();
    }

    @Test
    void should_StartEmpty_When_TrackerCreated() {
        assertThat(tracker.getCountTrue()).isEqualTo(0);
        assertThat(tracker.isFull()).isFalse();
        assertThat(tracker.isReady()).isFalse();
    }

    // =================================================================
    // addEntry Tests
    // =================================================================

    @Test
    void should_IncrementTrueCount_When_TrueAdded() {
        tracker.addEntry(true);

        assertThat(tracker.getCountTrue()).isEqualTo(1);
    }

    @Test
    void should_NotIncrementTrueCount_When_FalseAdded() {
        tracker.addEntry(false);

        assertThat(tracker.getCountTrue()).isEqualTo(0);
    }

    @Test
    void should_CountTrueValues_When_MixedEntriesAdded() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);
        tracker.addEntry(true);

        assertThat(tracker.getCountTrue()).isEqualTo(3);
    }

    @Test
    void should_HandleNullEntry_When_BooleanObjectPassed() {
        tracker.addEntry((Boolean) null);

        assertThat(tracker.getCountTrue()).isEqualTo(0);
    }

    @Test
    void should_AcceptBooleanObject_When_NonNullPassed() {
        tracker.addEntry(Boolean.TRUE);

        assertThat(tracker.getCountTrue()).isEqualTo(1);
    }

    // =================================================================
    // isFull Tests
    // =================================================================

    @Test
    void should_NotBeFull_When_LessThanCapacity() {
        tracker.addEntry(true);
        tracker.addEntry(false);

        assertThat(tracker.isFull()).isFalse();
    }

    @Test
    void should_BeFull_When_CapacityReached() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(i % 2 == 0);
        }

        assertThat(tracker.isFull()).isTrue();
    }

    @Test
    void should_RemainFull_When_AdditionalEntriesAdded() {
        for (int i = 0; i < 7; i++) {
            tracker.addEntry(true);
        }

        assertThat(tracker.isFull()).isTrue();
    }

    // =================================================================
    // isReady Tests
    // =================================================================

    @Test
    void should_NotBeReady_When_BelowMinimum() {
        tracker.addEntry(true);

        assertThat(tracker.isReady()).isFalse();
    }

    @Test
    void should_BeReady_When_AboveMinimum() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);

        assertThat(tracker.isReady()).isTrue();
    }

    @Test
    void should_BeReady_When_Full() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        assertThat(tracker.isReady()).isTrue();
    }

    // =================================================================
    // getCount Tests
    // =================================================================

    @Test
    void should_ReturnCapacity_When_GetCountCalled() {
        assertThat(tracker.getCount()).isEqualTo(5);
    }

    @Test
    void should_ReturnSameCapacity_When_EntriesAdded() {
        tracker.addEntry(true);
        tracker.addEntry(false);

        assertThat(tracker.getCount()).isEqualTo(5);
    }

    // =================================================================
    // getCountTrue Tests
    // =================================================================

    @Test
    void should_ReturnZero_When_NoTrueValues() {
        tracker.addEntry(false);
        tracker.addEntry(false);

        assertThat(tracker.getCountTrue()).isEqualTo(0);
    }

    @Test
    void should_CountAllTrue_When_AllEntriesTrue() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        assertThat(tracker.getCountTrue()).isEqualTo(5);
    }

    @Test
    void should_CountMixedValues_When_TrueAndFalseAdded() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);

        assertThat(tracker.getCountTrue()).isEqualTo(2);
    }

    // =================================================================
    // getPercentTrue Tests
    // =================================================================

    @Test
    void should_ReturnDefault_When_BelowMinimum() {
        tracker.addEntry(true);

        float result = tracker.getPercentTrue(99.0f);

        assertThat(result).isEqualTo(99.0f);
    }

    @Test
    void should_Calculate100Percent_When_AllTrue() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        float result = tracker.getPercentTrue(0.0f);

        assertThat(result).isEqualTo(1.0f);
    }

    @Test
    void should_Calculate0Percent_When_AllFalse() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(false);
        }

        float result = tracker.getPercentTrue(99.0f);

        assertThat(result).isEqualTo(0.0f);
    }

    @Test
    void should_Calculate50Percent_When_HalfTrue() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);
        tracker.addEntry(false);

        float result = tracker.getPercentTrue(0.0f);

        assertThat(result).isEqualTo(0.5f);
    }

    // =================================================================
    // getWeightedPercentTrue Tests
    // =================================================================

    @Test
    void should_ReturnDefaultWeighted_When_BelowMinimum() {
        tracker.addEntry(true);

        float result = tracker.getWeightedPercentTrue(99.0f);

        assertThat(result).isEqualTo(99.0f);
    }

    @Test
    void should_CalculateWeightedPercent_When_AboveMinimum() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);

        float result = tracker.getWeightedPercentTrue(0.0f);

        assertThat(result).isBetween(0.0f, 1.0f);
    }

    // =================================================================
    // clear Tests
    // =================================================================

    @Test
    void should_ResetTrueCount_When_Cleared() {
        tracker.addEntry(true);
        tracker.addEntry(true);

        tracker.clear();

        assertThat(tracker.getCountTrue()).isEqualTo(0);
    }

    @Test
    void should_ResetFullFlag_When_Cleared() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        tracker.clear();

        assertThat(tracker.isFull()).isFalse();
    }

    @Test
    void should_AllowNewEntries_When_ClearedAndReused() {
        tracker.addEntry(true);
        tracker.clear();

        tracker.addEntry(false);

        assertThat(tracker.getCountTrue()).isEqualTo(0);
    }

    // =================================================================
    // Circular Buffer Tests
    // =================================================================

    @Test
    void should_OverwriteOldest_When_CapacityExceeded() {
        // Fill with true
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        // Add false - should overwrite first true
        tracker.addEntry(false);

        assertThat(tracker.getCountTrue()).isEqualTo(4);
    }

    @Test
    void should_MaintainCorrectCount_When_CircularBufferWraps() {
        for (int i = 0; i < 10; i++) {
            tracker.addEntry(i % 2 == 0);
        }

        // Last 5 entries: false, true, false, true, false
        assertThat(tracker.getCountTrue()).isEqualTo(2);
    }

    // =================================================================
    // getConsecutive Tests
    // =================================================================

    @Test
    void should_ReturnOne_When_FirstValueMatches() {
        tracker.addEntry(true);
        tracker.addEntry(false);

        int result = tracker.getConsecutive(true);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void should_CountConsecutiveTrue_When_AllTrue() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        int result = tracker.getConsecutive(true);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void should_CountConsecutiveFalse_When_AllFalse() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(false);
        }

        int result = tracker.getConsecutive(false);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void should_CountFromStart_When_MixedValues() {
        tracker.addEntry(false);
        tracker.addEntry(false);
        tracker.addEntry(true);
        tracker.addEntry(true);
        tracker.addEntry(true);

        // getConsecutive counts from index 0 (first entry)
        int result = tracker.getConsecutive(false);

        assertThat(result).isEqualTo(2);
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ReturnString_When_ToStringCalled() {
        tracker.addEntry(true);

        String result = tracker.toString();

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_ReturnDifferentString_When_StateChanges() {
        tracker.addEntry(true);
        String before = tracker.toString();

        tracker.addEntry(false);
        String after = tracker.toString();

        assertThat(after).isNotEqualTo(before);
    }

    @Test
    void should_ShowAllEntries_When_NotFullToString() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);

        String result = tracker.toString();

        assertThat(result).contains("true");
        assertThat(result).contains("false");
    }

    @Test
    void should_ShowCircularOrder_When_FullToString() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(i % 2 == 0);
        }

        String result = tracker.toString();

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
    }

    // =================================================================
    // encode Tests
    // =================================================================

    @Test
    void should_ReturnEncodedString_When_EncodeCalled() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);

        Object encoded = tracker.encode();

        assertThat(encoded).isNotNull();
        assertThat(encoded).isInstanceOf(String.class);
    }

    @Test
    void should_EncodeEmptyTracker_When_NoEntries() {
        Object encoded = tracker.encode();

        assertThat(encoded).isNotNull();
        assertThat(encoded.toString()).contains("2,0,false,0,5,FFFFF");
    }

    @Test
    void should_EncodePartialTracker_When_SomeEntries() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);

        Object encoded = tracker.encode();
        String encodedStr = (String) encoded;

        assertThat(encodedStr).contains("2,3,false,2,5");
        assertThat(encodedStr).containsAnyOf("T", "F");
    }

    @Test
    void should_EncodeFullTracker_When_AtCapacity() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(i % 2 == 0);
        }

        Object encoded = tracker.encode();
        String encodedStr = (String) encoded;

        assertThat(encodedStr).contains("true");
        assertThat(encodedStr).contains("5,TFTFT");
    }

    @Test
    void should_EncodeOverflowedTracker_When_BeyondCapacity() {
        for (int i = 0; i < 7; i++) {
            tracker.addEntry(i % 2 == 0);
        }

        Object encoded = tracker.encode();

        assertThat(encoded).isNotNull();
        assertThat(encoded.toString()).contains("true");
    }

    // =================================================================
    // decode Tests
    // =================================================================

    @Test
    void should_RestoreState_When_ValidEncodedString() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);

        Object encoded = tracker.encode();

        BooleanTracker newTracker = new BooleanTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getCountTrue()).isEqualTo(tracker.getCountTrue());
        assertThat(newTracker.isFull()).isEqualTo(tracker.isFull());
    }

    @Test
    void should_HandleNull_When_DecodeWithNull() {
        tracker.decode(null);

        assertThat(tracker.getCountTrue()).isEqualTo(0);
    }

    @Test
    void should_DecodeEmptyTracker_When_EmptyEncoded() {
        BooleanTracker emptyTracker = new BooleanTracker(5, 2);
        Object encoded = emptyTracker.encode();

        BooleanTracker newTracker = new BooleanTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getCountTrue()).isEqualTo(0);
        assertThat(newTracker.isFull()).isFalse();
    }

    @Test
    void should_DecodeFullTracker_When_FullEncoded() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(i % 2 == 0);
        }

        Object encoded = tracker.encode();

        BooleanTracker newTracker = new BooleanTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.isFull()).isTrue();
        assertThat(newTracker.getCountTrue()).isEqualTo(tracker.getCountTrue());
    }

    @Test
    void should_PreservePercentages_When_DecodeAfterEncode() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);
        tracker.addEntry(true);
        tracker.addEntry(false);

        float originalPercent = tracker.getPercentTrue(0.0f);
        Object encoded = tracker.encode();

        BooleanTracker newTracker = new BooleanTracker(5, 2);
        newTracker.decode(encoded);

        float decodedPercent = newTracker.getPercentTrue(0.0f);

        assertThat(decodedPercent).isEqualTo(originalPercent);
    }

    @Test
    void should_PreserveWeightedPercent_When_DecodeAfterEncode() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);

        float originalWeighted = tracker.getWeightedPercentTrue(0.0f);
        Object encoded = tracker.encode();

        BooleanTracker newTracker = new BooleanTracker(5, 2);
        newTracker.decode(encoded);

        float decodedWeighted = newTracker.getWeightedPercentTrue(0.0f);

        assertThat(decodedWeighted).isEqualTo(originalWeighted);
    }

    // =================================================================
    // encode/decode Round-Trip Tests
    // =================================================================

    @Test
    void should_RoundTripSuccessfully_When_PartialTracker() {
        tracker.addEntry(true);
        tracker.addEntry(false);

        Object encoded = tracker.encode();
        BooleanTracker newTracker = new BooleanTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.toString()).isEqualTo(tracker.toString());
    }

    @Test
    void should_RoundTripSuccessfully_When_FullTracker() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(i % 3 == 0);
        }

        Object encoded = tracker.encode();
        BooleanTracker newTracker = new BooleanTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getCountTrue()).isEqualTo(tracker.getCountTrue());
        assertThat(newTracker.isFull()).isEqualTo(tracker.isFull());
        assertThat(newTracker.getPercentTrue(0.0f)).isEqualTo(tracker.getPercentTrue(0.0f));
    }

    @Test
    void should_RoundTripSuccessfully_When_AllTrue() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        Object encoded = tracker.encode();
        BooleanTracker newTracker = new BooleanTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getPercentTrue(0.0f)).isEqualTo(1.0f);
    }

    @Test
    void should_RoundTripSuccessfully_When_AllFalse() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(false);
        }

        Object encoded = tracker.encode();
        BooleanTracker newTracker = new BooleanTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getPercentTrue(0.0f)).isEqualTo(0.0f);
    }

    // =================================================================
    // Advanced getWeightedPercentTrue Tests
    // =================================================================

    @Test
    void should_WeightRecentHigher_When_CalculatingWeighted() {
        tracker.addEntry(false);
        tracker.addEntry(false);
        tracker.addEntry(true); // Most recent weighted higher

        float weighted = tracker.getWeightedPercentTrue(0.0f);
        float simple = tracker.getPercentTrue(0.0f);

        // Weighted should be higher because recent entry is true
        assertThat(weighted).isGreaterThan(simple);
    }

    @Test
    void should_Return100Percent_When_AllTrueWeighted() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        float result = tracker.getWeightedPercentTrue(0.0f);

        assertThat(result).isEqualTo(1.0f);
    }

    @Test
    void should_Return0Percent_When_AllFalseWeighted() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(false);
        }

        float result = tracker.getWeightedPercentTrue(0.0f);

        assertThat(result).isEqualTo(0.0f);
    }

    @Test
    void should_WeightCircularBuffer_When_FullAndWrapped() {
        // Fill completely
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(false);
        }

        // Add true values - these should be weighted higher
        tracker.addEntry(true);
        tracker.addEntry(true);

        float weighted = tracker.getWeightedPercentTrue(0.0f);

        // Should have some true weight despite only 2 true in buffer of 5
        assertThat(weighted).isGreaterThan(0.0f);
    }

    // =================================================================
    // Edge Cases for Circular Buffer Count Updates
    // =================================================================

    @Test
    void should_UpdateCountCorrectly_When_OverwritingSameValue() {
        // Fill with true
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        assertThat(tracker.getCountTrue()).isEqualTo(5);

        // Overwrite with true (same value)
        tracker.addEntry(true);

        assertThat(tracker.getCountTrue()).isEqualTo(5);
    }

    @Test
    void should_UpdateCountCorrectly_When_OverwritingDifferentValue() {
        // Fill with true
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }

        assertThat(tracker.getCountTrue()).isEqualTo(5);

        // Overwrite oldest true with false
        tracker.addEntry(false);

        assertThat(tracker.getCountTrue()).isEqualTo(4);
    }

    @Test
    void should_UpdateCountCorrectly_When_MultipleWraps() {
        // Add alternating pattern
        for (int i = 0; i < 15; i++) {
            tracker.addEntry(i % 2 == 0);
        }

        // Last 5: true(10), false(11), true(12), false(13), true(14)
        assertThat(tracker.getCountTrue()).isEqualTo(3);
    }
}
