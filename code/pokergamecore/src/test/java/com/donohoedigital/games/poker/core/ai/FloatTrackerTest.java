/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.poker.core.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FloatTrackerTest {

    private FloatTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new FloatTracker(5, 2);
    }

    @Test
    void newTrackerIsNotFullOrReady() {
        assertThat(tracker.isFull()).isFalse();
        assertThat(tracker.isReady()).isFalse();
        assertThat(tracker.getCount()).isZero();
    }

    @Test
    void returnsDefaultWhenBelowMinimum() {
        tracker.addEntry(1.0f);
        assertThat(tracker.isReady()).isFalse();
        assertThat(tracker.getWeightedAverage(0.5f)).isEqualTo(0.5f);
    }

    @Test
    void becomesReadyAfterMinEntries() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);
        assertThat(tracker.isReady()).isTrue();
        assertThat(tracker.isFull()).isFalse();
        assertThat(tracker.getCount()).isEqualTo(3);
    }

    @Test
    void becomesFullAfterWraparound() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
        }
        assertThat(tracker.isFull()).isTrue();
        assertThat(tracker.getCount()).isEqualTo(5);
    }

    @Test
    void weightedAverageGivesMoreWeightToRecent() {
        // Add 0, 0, 0, 10, 10 - recent values are higher
        tracker.addEntry(0.0f);
        tracker.addEntry(0.0f);
        tracker.addEntry(0.0f);
        tracker.addEntry(10.0f);
        tracker.addEntry(10.0f);

        float avg = tracker.getWeightedAverage(0.0f);
        // Weighted average should be > simple average (4.0) since recent is higher
        assertThat(avg).isGreaterThan(4.0f);
    }

    @Test
    void uniformValuesGiveExactAverage() {
        tracker.addEntry(5.0f);
        tracker.addEntry(5.0f);
        tracker.addEntry(5.0f);
        assertThat(tracker.getWeightedAverage(0.0f)).isCloseTo(5.0f, within(0.001f));
    }

    @Test
    void nullFloatIsIgnored() {
        tracker.addEntry(1.0f);
        tracker.addEntry((Float) null);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);
        assertThat(tracker.getCount()).isEqualTo(3);
    }

    @Test
    void clearResetsState() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        tracker.addEntry(3.0f);
        tracker.clear();
        assertThat(tracker.isFull()).isFalse();
        assertThat(tracker.isReady()).isFalse();
        assertThat(tracker.getCount()).isZero();
    }

    @Test
    void encodeAndDecode() {
        tracker.addEntry(1.5f);
        tracker.addEntry(2.5f);
        tracker.addEntry(3.5f);

        Object encoded = tracker.encode();
        assertThat(encoded).isInstanceOf(String.class);

        FloatTracker restored = new FloatTracker(5, 2);
        restored.decode(encoded);

        assertThat(restored.getCount()).isEqualTo(tracker.getCount());
        assertThat(restored.isReady()).isEqualTo(tracker.isReady());
        assertThat(restored.isFull()).isEqualTo(tracker.isFull());
        assertThat(restored.getWeightedAverage(0.0f)).isCloseTo(tracker.getWeightedAverage(0.0f), within(0.001f));
    }

    @Test
    void decodeNullIsNoOp() {
        tracker.addEntry(1.0f);
        tracker.decode(null);
        assertThat(tracker.getCount()).isEqualTo(1);
    }

    @Test
    void circularBufferOverwrite() {
        // Fill with 1.0
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(1.0f);
        }
        assertThat(tracker.getWeightedAverage(0.0f)).isCloseTo(1.0f, within(0.001f));

        // Overwrite oldest with 10.0
        tracker.addEntry(10.0f);
        // Now buffer has: 10.0, 1.0, 1.0, 1.0, 1.0 (oldest to newest in order)
        // Wait - circular: entries are [10, 1, 1, 1, 1] with next_=1, start=1
        // Reading order: 1, 1, 1, 1, 10 (oldest to newest)
        float avg = tracker.getWeightedAverage(0.0f);
        // Recent entry (10.0) has highest weight, so avg should be > simple average
        assertThat(avg).isGreaterThan(1.0f);
    }

    @Test
    void toStringNotEmpty() {
        tracker.addEntry(1.0f);
        tracker.addEntry(2.0f);
        String s = tracker.toString();
        assertThat(s).startsWith("[").endsWith("]");
    }

    @Test
    void toStringWhenFull() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry((float) i);
        }
        String s = tracker.toString();
        assertThat(s).startsWith("[").endsWith("]");
    }

    @Test
    void singleElementTracker() {
        FloatTracker tiny = new FloatTracker(1, 0);
        assertThat(tiny.isReady()).isFalse();
        tiny.addEntry(7.5f);
        assertThat(tiny.isFull()).isTrue();
        assertThat(tiny.getWeightedAverage(0.0f)).isCloseTo(7.5f, within(0.001f));
    }
}
