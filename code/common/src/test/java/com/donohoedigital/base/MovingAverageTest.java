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
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for MovingAverage - Circular buffer moving average calculation.
 */
class MovingAverageTest {

    // =================================================================
    // Basic Recording Tests
    // =================================================================

    @Test
    void should_StartAtZero_When_Initialized() {
        MovingAverage avg = new MovingAverage(5);

        assertThat(avg.getAverageLong()).isZero();
        assertThat(avg.getAverageDouble()).isZero();
        assertThat(avg.getPeak()).isZero();
        assertThat(avg.getHigh()).isZero();
    }

    @Test
    void should_RecordSingleValue_When_OneValueAdded() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(10);

        assertThat(avg.getAverageLong()).isEqualTo(10);
        assertThat(avg.getAverageDouble()).isEqualTo(10.0);
        assertThat(avg.getPeak()).isEqualTo(10);
        assertThat(avg.getHigh()).isEqualTo(10);
    }

    @Test
    void should_CalculateAverage_When_MultipleValuesAdded() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(10);
        avg.record(20);
        avg.record(30);

        assertThat(avg.getAverageLong()).isEqualTo(20); // (10+20+30)/3 = 20
        assertThat(avg.getAverageDouble()).isEqualTo(20.0);
    }

    @Test
    void should_CalculateCorrectly_When_BufferFilled() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(10);
        avg.record(20);
        avg.record(30);
        avg.record(40);
        avg.record(50);

        // All 5 slots filled: (10+20+30+40+50)/5 = 30
        assertThat(avg.getAverageLong()).isEqualTo(30);
    }

    // =================================================================
    // Circular Buffer Tests
    // =================================================================

    @Test
    void should_WrapAround_When_ExceedingBufferSize() {
        MovingAverage avg = new MovingAverage(3);
        avg.record(10);
        avg.record(20);
        avg.record(30);
        avg.record(40); // Replaces 10

        // Buffer now: [40, 20, 30] -> average = (40+20+30)/3 = 30
        assertThat(avg.getAverageLong()).isEqualTo(30);
    }

    @Test
    void should_MaintainCorrectAverage_When_CircularBufferWraps() {
        MovingAverage avg = new MovingAverage(3);
        avg.record(10);
        avg.record(20);
        avg.record(30);
        avg.record(40); // Replaces 10
        avg.record(50); // Replaces 20
        avg.record(60); // Replaces 30

        // Buffer now: [40, 50, 60] -> average = (40+50+60)/3 = 50
        assertThat(avg.getAverageLong()).isEqualTo(50);
    }

    // =================================================================
    // Peak Tracking Tests
    // =================================================================

    @Test
    void should_TrackPeak_When_ValuesRecorded() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(10);
        avg.record(30);
        avg.record(20);

        assertThat(avg.getPeak()).isEqualTo(30);
    }

    @Test
    void should_UpdatePeak_When_HigherValueRecorded() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(10);
        avg.record(30);
        avg.record(50);
        avg.record(20);

        assertThat(avg.getPeak()).isEqualTo(50);
    }

    @Test
    void should_MaintainPeak_When_LowerValuesRecorded() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(50);
        avg.record(10);
        avg.record(20);
        avg.record(5);

        // Peak stays at 50 even after recording lower values
        assertThat(avg.getPeak()).isEqualTo(50);
    }

    // =================================================================
    // High (Current Maximum) Tests
    // =================================================================

    @Test
    void should_TrackHigh_When_ValuesInBuffer() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(10);
        avg.record(30);
        avg.record(20);

        // High is the maximum of current buffer values
        assertThat(avg.getHigh()).isEqualTo(30);
    }

    @Test
    void should_UpdateHigh_When_OldHighValueEvicted() {
        MovingAverage avg = new MovingAverage(3);
        avg.record(50); // High value
        avg.record(10);
        avg.record(20);
        avg.record(30); // Replaces 50

        // Buffer now: [30, 10, 20] -> high = 30
        assertThat(avg.getHigh()).isEqualTo(30);
    }

    @Test
    void should_DifferFromPeak_When_HighValueEvicted() {
        MovingAverage avg = new MovingAverage(3);
        avg.record(100); // Peak
        avg.record(10);
        avg.record(20);
        avg.record(30); // Replaces 100

        // Peak remains 100, but high is now 30
        assertThat(avg.getPeak()).isEqualTo(100);
        assertThat(avg.getHigh()).isEqualTo(30);
    }

    // =================================================================
    // Reset Tests
    // =================================================================

    @Test
    void should_ClearAllValues_When_Reset() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(10);
        avg.record(20);
        avg.record(30);

        avg.reset();

        assertThat(avg.getAverageLong()).isZero();
        assertThat(avg.getPeak()).isZero();
        assertThat(avg.getHigh()).isZero();
    }

    @Test
    void should_StartFresh_When_ResetThenRecord() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(10);
        avg.record(20);
        avg.reset();
        avg.record(5);

        assertThat(avg.getAverageLong()).isEqualTo(5);
        assertThat(avg.getPeak()).isEqualTo(5);
    }

    // =================================================================
    // Double vs Long Average Tests
    // =================================================================

    @Test
    void should_RoundDown_When_GetAverageLong() {
        MovingAverage avg = new MovingAverage(3);
        avg.record(10);
        avg.record(11);

        // Average = 21/2 = 10.5, rounded down to 10
        assertThat(avg.getAverageLong()).isEqualTo(10);
        assertThat(avg.getAverageDouble()).isEqualTo(10.5);
    }

    @Test
    void should_PreservePrecision_When_GetAverageDouble() {
        MovingAverage avg = new MovingAverage(3);
        avg.record(10);
        avg.record(20);
        avg.record(25);

        // Average = 55/3 = 18.333...
        assertThat(avg.getAverageDouble()).isEqualTo(55.0 / 3.0);
    }

    // =================================================================
    // Edge Cases Tests
    // =================================================================

    @Test
    void should_HandleZeroValues_When_ZerosRecorded() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(0);
        avg.record(0);
        avg.record(0);

        assertThat(avg.getAverageLong()).isZero();
        assertThat(avg.getPeak()).isZero();
    }

    @Test
    void should_HandleLargeValues_When_LargeNumbersRecorded() {
        MovingAverage avg = new MovingAverage(3);
        avg.record(1_000_000);
        avg.record(2_000_000);
        avg.record(3_000_000);

        assertThat(avg.getAverageLong()).isEqualTo(2_000_000);
        assertThat(avg.getPeak()).isEqualTo(3_000_000);
    }

    @Test
    void should_HandleSingleSlotBuffer_When_BufferSizeOne() {
        MovingAverage avg = new MovingAverage(1);
        avg.record(10);
        avg.record(20);

        // Only last value retained
        assertThat(avg.getAverageLong()).isEqualTo(20);
    }

    @Test
    void should_HandleMixedValues_When_PositiveAndZero() {
        MovingAverage avg = new MovingAverage(4);
        avg.record(10);
        avg.record(0);
        avg.record(20);
        avg.record(0);

        // Average = (10+0+20+0)/4 = 7.5
        assertThat(avg.getAverageLong()).isEqualTo(7);
        assertThat(avg.getAverageDouble()).isEqualTo(7.5);
    }

    // =================================================================
    // Calculation Accuracy Tests
    // =================================================================

    @Test
    void should_CalculateCorrectly_When_PartiallyFilled() {
        MovingAverage avg = new MovingAverage(10);
        avg.record(10);
        avg.record(20);
        avg.record(30);

        // Only 3 of 10 slots used: (10+20+30)/3 = 20
        assertThat(avg.getAverageLong()).isEqualTo(20);
    }

    @Test
    void should_HandleSequentialWrites_When_MultipleRotations() {
        MovingAverage avg = new MovingAverage(2);

        // First rotation
        avg.record(10);
        avg.record(20);
        assertThat(avg.getAverageLong()).isEqualTo(15);

        // Second rotation
        avg.record(30);
        assertThat(avg.getAverageLong()).isEqualTo(25); // (30+20)/2

        // Third rotation
        avg.record(40);
        assertThat(avg.getAverageLong()).isEqualTo(35); // (30+40)/2
    }

    @Test
    void should_MaintainIntegerSum_When_CalculatingAverage() {
        MovingAverage avg = new MovingAverage(5);
        avg.record(1);
        avg.record(2);
        avg.record(3);
        avg.record(4);
        avg.record(5);

        // Sum = 15, count = 5, average = 3
        assertThat(avg.getAverageLong()).isEqualTo(3);
        assertThat(avg.getAverageDouble()).isEqualTo(3.0);
    }
}
