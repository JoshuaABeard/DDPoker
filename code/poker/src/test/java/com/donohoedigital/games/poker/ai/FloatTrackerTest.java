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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for FloatTracker - tracks float values with weighted average calculation.
 */
class FloatTrackerTest {

    private FloatTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new FloatTracker(5, 2);
    }

    // =================================================================
    // Constructor and Initialization Tests
    // =================================================================

    @Test
    void should_CreateTracker_When_ValidParametersProvided() {
        FloatTracker newTracker = new FloatTracker(10, 3);

        assertThat(newTracker).isNotNull();
        assertThat(newTracker.getCount()).isEqualTo(0);
        assertThat(newTracker.isFull()).isFalse();
    }

    @Test
    void should_StartEmpty_When_TrackerCreated() {
        assertThat(tracker.getCount()).isEqualTo(0);
        assertThat(tracker.isFull()).isFalse();
        assertThat(tracker.isReady()).isFalse();
    }

    // =================================================================
    // addEntry Tests
    // =================================================================

    @Test
    void should_IncrementCount_When_EntryAdded() {
        tracker.addEntry(1.0f);

        assertThat(tracker.getCount()).isEqualTo(1);
    }

    @Test
    void should_AcceptMultipleEntries_When_AddedSequentially() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);

        assertThat(tracker.getCount()).isEqualTo(3);
    }

    @Test
    void should_HandleNullEntry_When_FloatObjectPassed() {
        tracker.addEntry((Float) null);

        assertThat(tracker.getCount()).isEqualTo(0);
    }

    @Test
    void should_AcceptFloatObject_When_NonNullPassed() {
        tracker.addEntry(Float.valueOf(2.5f));

        assertThat(tracker.getCount()).isEqualTo(1);
    }

    // =================================================================
    // isFull Tests
    // =================================================================

    @Test
    void should_NotBeFull_When_LessThanCapacity() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);

        assertThat(tracker.isFull()).isFalse();
    }

    @Test
    void should_BeFull_When_CapacityReached() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
        }

        assertThat(tracker.isFull()).isTrue();
    }

    @Test
    void should_RemainFull_When_AdditionalEntriesAdded() {
        for (int i = 0; i < 7; i++) {
            tracker.addEntry((float) i);
        }

        assertThat(tracker.isFull()).isTrue();
        assertThat(tracker.getCount()).isEqualTo(5);
    }

    // =================================================================
    // isReady Tests
    // =================================================================

    @Test
    void should_NotBeReady_When_BelowMinimum() {
        tracker.addEntry(1.0f);

        assertThat(tracker.isReady()).isFalse();
    }

    @Test
    void should_BeReady_When_MinimumReached() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);

        assertThat(tracker.isReady()).isFalse(); // min is 2, but isReady requires > min

        tracker.addEntry(3.0f);

        assertThat(tracker.isReady()).isTrue();
    }

    @Test
    void should_BeReady_When_Full() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
        }

        assertThat(tracker.isReady()).isTrue();
    }

    // =================================================================
    // getCount Tests
    // =================================================================

    @Test
    void should_ReturnZero_When_NoEntriesAdded() {
        assertThat(tracker.getCount()).isEqualTo(0);
    }

    @Test
    void should_ReturnCorrectCount_When_PartiallyFilled() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);

        assertThat(tracker.getCount()).isEqualTo(3);
    }

    @Test
    void should_ReturnCapacity_When_Full() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
        }

        assertThat(tracker.getCount()).isEqualTo(5);
    }

    @Test
    void should_ReturnCapacity_When_OverFilled() {
        for (int i = 0; i < 10; i++) {
            tracker.addEntry((float) i);
        }

        assertThat(tracker.getCount()).isEqualTo(5);
    }

    // =================================================================
    // getWeightedAverage Tests
    // =================================================================

    @Test
    void should_ReturnDefault_When_BelowMinimum() {
        tracker.addEntry(1.0f);

        float result = tracker.getWeightedAverage(99.0f);

        assertThat(result).isEqualTo(99.0f);
    }

    @Test
    void should_CalculateAverage_When_AboveMinimum() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);

        float result = tracker.getWeightedAverage(99.0f);

        assertThat(result).isNotEqualTo(99.0f);
        assertThat(result).isGreaterThan(0);
    }

    @Test
    void should_UpdateAverage_When_NewEntriesAdded() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);

        float firstAverage = tracker.getWeightedAverage(0.0f);

        tracker.addEntry(10.0f);

        float secondAverage = tracker.getWeightedAverage(0.0f);

        assertThat(secondAverage).isNotEqualTo(firstAverage);
    }

    // =================================================================
    // clear Tests
    // =================================================================

    @Test
    void should_ResetCount_When_Cleared() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);

        tracker.clear();

        assertThat(tracker.getCount()).isEqualTo(0);
    }

    @Test
    void should_ResetFullFlag_When_Cleared() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
        }

        tracker.clear();

        assertThat(tracker.isFull()).isFalse();
    }

    @Test
    void should_AllowNewEntries_When_ClearedAndReused() {
        tracker.addEntry(1.0f);
        tracker.clear();

        tracker.addEntry(2.0f);

        assertThat(tracker.getCount()).isEqualTo(1);
    }

    // =================================================================
    // Circular Buffer Tests
    // =================================================================

    @Test
    void should_OverwriteOldest_When_CapacityExceeded() {
        // Fill tracker
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
        }

        // Add one more - should overwrite oldest
        tracker.addEntry(99.0f);

        assertThat(tracker.getCount()).isEqualTo(5);
        assertThat(tracker.isFull()).isTrue();
    }

    @Test
    void should_MaintainCount_When_CircularBufferWraps() {
        for (int i = 0; i < 10; i++) {
            tracker.addEntry((float) i);
        }

        assertThat(tracker.getCount()).isEqualTo(5);
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_HandleZeroValues_When_Added() {
        tracker.addEntry(0.0f);
        tracker.addEntry(0.0f);
        tracker.addEntry(0.0f);

        assertThat(tracker.getCount()).isEqualTo(3);
    }

    @Test
    void should_HandleNegativeValues_When_Added() {
        tracker.addEntry(-1.0f);
        tracker.addEntry(-2.0f);
        tracker.addEntry(-3.0f);

        assertThat(tracker.getCount()).isEqualTo(3);
    }

    @Test
    void should_HandleLargeValues_When_Added() {
        tracker.addEntry(Float.MAX_VALUE);
        tracker.addEntry(Float.MAX_VALUE / 2);

        assertThat(tracker.getCount()).isEqualTo(2);
    }

    @Test
    void should_HandleSmallValues_When_Added() {
        tracker.addEntry(Float.MIN_VALUE);
        tracker.addEntry(Float.MIN_VALUE * 2);

        assertThat(tracker.getCount()).isEqualTo(2);
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ReturnString_When_ToStringCalled() {
        tracker.addEntry(1.0f);

        String result = tracker.toString();

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_ReturnDifferentString_When_StateChanges() {
        tracker.addEntry(1.0f);
        String before = tracker.toString();

        tracker.addEntry(2.0f);
        String after = tracker.toString();

        assertThat(after).isNotEqualTo(before);
    }

    @Test
    void should_ShowAllEntries_When_NotFullToString() {
        tracker.addEntry(1.5f);
        tracker.addEntry(2.5f);
        tracker.addEntry(3.5f);

        String result = tracker.toString();

        assertThat(result).contains("1.5");
        assertThat(result).contains("2.5");
        assertThat(result).contains("3.5");
    }

    @Test
    void should_ShowCircularOrder_When_FullToString() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
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
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);

        Object encoded = tracker.encode();

        assertThat(encoded).isNotNull();
        assertThat(encoded).isInstanceOf(String.class);
    }

    @Test
    void should_EncodeEmptyTracker_When_NoEntries() {
        Object encoded = tracker.encode();

        assertThat(encoded).isNotNull();
        assertThat(encoded.toString()).contains("2,0,false");
    }

    @Test
    void should_EncodePartialTracker_When_SomeEntries() {
        tracker.addEntry(1.5f);
        tracker.addEntry(2.5f);
        tracker.addEntry(3.5f);

        Object encoded = tracker.encode();
        String encodedStr = (String) encoded;

        assertThat(encodedStr).contains("2,3,false");
        assertThat(encodedStr).contains(":");
    }

    @Test
    void should_EncodeFullTracker_When_AtCapacity() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
        }

        Object encoded = tracker.encode();
        String encodedStr = (String) encoded;

        assertThat(encodedStr).contains("true");
        assertThat(encodedStr).contains(":");
    }

    @Test
    void should_EncodeOverflowedTracker_When_BeyondCapacity() {
        for (int i = 0; i < 7; i++) {
            tracker.addEntry((float) i);
        }

        Object encoded = tracker.encode();

        assertThat(encoded).isNotNull();
        assertThat(encoded.toString()).contains("true");
    }

    @Test
    void should_EncodeZeroValues_When_ZerosAdded() {
        tracker.addEntry(0.0f);
        tracker.addEntry(0.0f);
        tracker.addEntry(0.0f);

        Object encoded = tracker.encode();

        assertThat(encoded).isNotNull();
        assertThat(encoded.toString()).contains("0.0");
    }

    @Test
    void should_EncodeNegativeValues_When_NegativesAdded() {
        tracker.addEntry(-1.5f);
        tracker.addEntry(-2.5f);

        Object encoded = tracker.encode();
        String encodedStr = (String) encoded;

        assertThat(encodedStr).contains("-");
    }

    // =================================================================
    // decode Tests
    // =================================================================

    @Test
    void should_RestoreState_When_ValidEncodedString() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);

        Object encoded = tracker.encode();

        FloatTracker newTracker = new FloatTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getCount()).isEqualTo(tracker.getCount());
        assertThat(newTracker.isFull()).isEqualTo(tracker.isFull());
    }

    @Test
    void should_HandleNull_When_DecodeWithNull() {
        tracker.decode(null);

        assertThat(tracker.getCount()).isEqualTo(0);
    }

    @Test
    void should_DecodeEmptyTracker_When_EmptyEncoded() {
        FloatTracker emptyTracker = new FloatTracker(5, 2);
        Object encoded = emptyTracker.encode();

        FloatTracker newTracker = new FloatTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getCount()).isEqualTo(0);
        assertThat(newTracker.isFull()).isFalse();
    }

    @Test
    void should_DecodeFullTracker_When_FullEncoded() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
        }

        Object encoded = tracker.encode();

        FloatTracker newTracker = new FloatTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.isFull()).isTrue();
        assertThat(newTracker.getCount()).isEqualTo(tracker.getCount());
    }

    @Test
    void should_PreserveAverage_When_DecodeAfterEncode() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);
        tracker.addEntry(4.0f);
        tracker.addEntry(5.0f);

        float originalAvg = tracker.getWeightedAverage(0.0f);
        Object encoded = tracker.encode();

        FloatTracker newTracker = new FloatTracker(5, 2);
        newTracker.decode(encoded);

        float decodedAvg = newTracker.getWeightedAverage(0.0f);

        assertThat(decodedAvg).isEqualTo(originalAvg);
    }

    @Test
    void should_RestoreNegativeValues_When_DecodedNegatives() {
        tracker.addEntry(-1.0f);
        tracker.addEntry(-2.0f);
        tracker.addEntry(-3.0f);

        Object encoded = tracker.encode();

        FloatTracker newTracker = new FloatTracker(5, 2);
        newTracker.decode(encoded);

        float decodedAvg = newTracker.getWeightedAverage(99.0f);

        assertThat(decodedAvg).isLessThan(0.0f);
    }

    // =================================================================
    // encode/decode Round-Trip Tests
    // =================================================================

    @Test
    void should_RoundTripSuccessfully_When_PartialTracker() {
        tracker.addEntry(1.5f);
        tracker.addEntry(2.5f);

        Object encoded = tracker.encode();
        FloatTracker newTracker = new FloatTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.toString()).isEqualTo(tracker.toString());
    }

    @Test
    void should_RoundTripSuccessfully_When_FullTracker() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i * 1.5f);
        }

        Object encoded = tracker.encode();
        FloatTracker newTracker = new FloatTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getCount()).isEqualTo(tracker.getCount());
        assertThat(newTracker.isFull()).isEqualTo(tracker.isFull());
        assertThat(newTracker.getWeightedAverage(0.0f)).isEqualTo(tracker.getWeightedAverage(0.0f));
    }

    @Test
    void should_RoundTripSuccessfully_When_MixedValues() {
        tracker.addEntry(1.0f);
        tracker.addEntry(-2.5f);
        tracker.addEntry(3.7f);
        tracker.addEntry(0.0f);
        tracker.addEntry(-1.2f);

        Object encoded = tracker.encode();
        FloatTracker newTracker = new FloatTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getWeightedAverage(99.0f)).isEqualTo(tracker.getWeightedAverage(99.0f));
    }

    @Test
    void should_RoundTripSuccessfully_When_LargeValues() {
        tracker.addEntry(1000.5f);
        tracker.addEntry(2000.7f);
        tracker.addEntry(3000.9f);

        Object encoded = tracker.encode();
        FloatTracker newTracker = new FloatTracker(5, 2);
        newTracker.decode(encoded);

        assertThat(newTracker.getWeightedAverage(0.0f)).isCloseTo(tracker.getWeightedAverage(0.0f), within(0.01f));
    }

    // =================================================================
    // Advanced Weighted Average Tests
    // =================================================================

    @Test
    void should_WeightRecentHigher_When_CalculatingAverage() {
        tracker.addEntry(1.0f);
        tracker.addEntry(1.0f);
        tracker.addEntry(10.0f); // Most recent weighted higher

        float weighted = tracker.getWeightedAverage(0.0f);

        // Weighted average should be > simple average (4.0) because recent value is high
        assertThat(weighted).isGreaterThan(4.0f);
    }

    @Test
    void should_CalculateCorrectAverage_When_SingleEntry() {
        tracker.addEntry(5.0f);
        tracker.addEntry(5.0f);
        tracker.addEntry(5.0f);

        float result = tracker.getWeightedAverage(0.0f);

        assertThat(result).isEqualTo(5.0f);
    }

    @Test
    void should_UpdateAverageAfterClear_When_NewEntriesAdded() {
        tracker.addEntry(10.0f);
        tracker.addEntry(10.0f);
        tracker.addEntry(10.0f);

        tracker.clear();

        tracker.addEntry(1.0f);
        tracker.addEntry(1.0f);
        tracker.addEntry(1.0f);

        float result = tracker.getWeightedAverage(0.0f);

        assertThat(result).isCloseTo(1.0f, within(0.01f));
    }

    @Test
    void should_WeightCircularBuffer_When_FullAndWrapped() {
        // Fill with low values
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(1.0f);
        }

        // Add high values - these should be weighted higher
        tracker.addEntry(10.0f);
        tracker.addEntry(10.0f);

        float weighted = tracker.getWeightedAverage(0.0f);

        // Most recent entries are 10, 10, 1, 1, 1
        // Weighted should be higher than simple average (6.4)
        assertThat(weighted).isGreaterThan(6.0f);
    }

    @Test
    void should_CalculateAverage_When_AtMinimum() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);

        // 2 entries, min is 2, so next_ (2) >= min_ (2) - calculates average
        float result = tracker.getWeightedAverage(99.0f);

        // Weighted: 1*1 + 2*2 = 5, div = 1+2 = 3, result = 5/3 = 1.667
        assertThat(result).isCloseTo(1.667f, within(0.01f));
    }

    @Test
    void should_CalculateWeightedAverage_When_JustBeyondMinimum() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);

        // 3 entries, min is 2, so next_ (3) > min_ (2)
        float result = tracker.getWeightedAverage(99.0f);

        assertThat(result).isNotEqualTo(99.0f);
        assertThat(result).isGreaterThan(0.0f);
    }

    // =================================================================
    // Weighted Average Formula Verification Tests
    // =================================================================

    @Test
    void should_CalculateCorrectWeightedAverage_When_ThreeEntries() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);

        // Weights: 1, 2, 3 (most recent has highest weight)
        // sum = 1*1 + 2*2 + 3*3 = 1 + 4 + 9 = 14
        // div = 1 + 2 + 3 = 6
        // weighted = 14/6 = 2.333...

        float result = tracker.getWeightedAverage(0.0f);

        assertThat(result).isCloseTo(2.333f, within(0.01f));
    }

    @Test
    void should_CalculateCorrectWeightedAverage_When_FullBuffer() {
        for (int i = 1; i <= 5; i++) {
            tracker.addEntry((float) i);
        }

        // Values: 1, 2, 3, 4, 5
        // Weights: 1, 2, 3, 4, 5
        // sum = 1*1 + 2*2 + 3*3 + 4*4 + 5*5 = 1+4+9+16+25 = 55
        // div = 1+2+3+4+5 = 15
        // weighted = 55/15 = 3.666...

        float result = tracker.getWeightedAverage(0.0f);

        assertThat(result).isCloseTo(3.667f, within(0.01f));
    }

    @Test
    void should_CalculateCorrectWeightedAverage_When_AfterWrap() {
        // Fill buffer
        for (int i = 1; i <= 5; i++) {
            tracker.addEntry((float) i);
        }

        // Add one more to wrap
        tracker.addEntry(10.0f);

        // Buffer now contains: 2, 3, 4, 5, 10 (10 is at index 0, most recent)
        // Order from oldest to newest: 2, 3, 4, 5, 10
        // Weights: 1, 2, 3, 4, 5
        // sum = 2*1 + 3*2 + 4*3 + 5*4 + 10*5 = 2+6+12+20+50 = 90
        // div = 1+2+3+4+5 = 15
        // weighted = 90/15 = 6.0

        float result = tracker.getWeightedAverage(0.0f);

        assertThat(result).isCloseTo(6.0f, within(0.01f));
    }
}
