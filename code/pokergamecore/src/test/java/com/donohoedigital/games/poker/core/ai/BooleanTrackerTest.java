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

class BooleanTrackerTest {

    private BooleanTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new BooleanTracker(5, 2);
    }

    @Test
    void newTrackerIsNotFullOrReady() {
        assertThat(tracker.isFull()).isFalse();
        assertThat(tracker.isReady()).isFalse();
        assertThat(tracker.getCountTrue()).isZero();
    }

    @Test
    void returnsDefaultWhenBelowMinimum() {
        tracker.addEntry(true);
        assertThat(tracker.isReady()).isFalse();
        assertThat(tracker.getPercentTrue(0.5f)).isEqualTo(0.5f);
        assertThat(tracker.getWeightedPercentTrue(0.5f)).isEqualTo(0.5f);
    }

    @Test
    void becomesReadyAfterMinEntries() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);
        assertThat(tracker.isReady()).isTrue();
        assertThat(tracker.isFull()).isFalse();
    }

    @Test
    void percentTrueBeforeFull() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);
        // 2 true out of 3 entries
        assertThat(tracker.getPercentTrue(0.0f)).isCloseTo(2.0f / 3.0f, within(0.001f));
    }

    @Test
    void becomesFullAfterWraparound() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(i % 2 == 0); // true, false, true, false, true
        }
        assertThat(tracker.isFull()).isTrue();
        assertThat(tracker.getCountTrue()).isEqualTo(3);
        assertThat(tracker.getPercentTrue(0.0f)).isCloseTo(0.6f, within(0.001f));
    }

    @Test
    void circularBufferOverwrite() {
        // Fill buffer: true, true, true, true, true
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(true);
        }
        assertThat(tracker.getCountTrue()).isEqualTo(5);

        // Overwrite first entry with false
        tracker.addEntry(false);
        assertThat(tracker.getCountTrue()).isEqualTo(4);
        assertThat(tracker.getPercentTrue(0.0f)).isCloseTo(0.8f, within(0.001f));
    }

    @Test
    void nullBooleanIsIgnored() {
        tracker.addEntry(true);
        tracker.addEntry((Boolean) null);
        tracker.addEntry(true);
        tracker.addEntry(false);
        // null should not have advanced the index
        assertThat(tracker.isReady()).isTrue();
        assertThat(tracker.getCountTrue()).isEqualTo(2);
    }

    @Test
    void clearResetsState() {
        tracker.addEntry(true);
        tracker.addEntry(true);
        tracker.addEntry(true);
        tracker.clear();
        assertThat(tracker.isFull()).isFalse();
        assertThat(tracker.isReady()).isFalse();
        assertThat(tracker.getCountTrue()).isZero();
    }

    @Test
    void encodeAndDecode() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        tracker.addEntry(true);

        Object encoded = tracker.encode();
        assertThat(encoded).isInstanceOf(String.class);

        BooleanTracker restored = new BooleanTracker(5, 2);
        restored.decode(encoded);

        assertThat(restored.getCountTrue()).isEqualTo(tracker.getCountTrue());
        assertThat(restored.isReady()).isEqualTo(tracker.isReady());
        assertThat(restored.isFull()).isEqualTo(tracker.isFull());
        assertThat(restored.getPercentTrue(0.5f)).isCloseTo(tracker.getPercentTrue(0.5f), within(0.001f));
    }

    @Test
    void decodeNullIsNoOp() {
        tracker.addEntry(true);
        tracker.decode(null);
        // Should not throw, state unchanged
        assertThat(tracker.getCountTrue()).isEqualTo(1);
    }

    @Test
    void weightedPercentTrueGivesMoreWeightToRecent() {
        // Add false, false, false, true, true (recent entries are true)
        tracker.addEntry(false);
        tracker.addEntry(false);
        tracker.addEntry(false);
        tracker.addEntry(true);
        tracker.addEntry(true);

        float weighted = tracker.getWeightedPercentTrue(0.5f);
        float unweighted = tracker.getPercentTrue(0.5f);

        // Weighted should be > unweighted since recent entries are true
        assertThat(weighted).isGreaterThan(unweighted);
    }

    @Test
    void consecutiveTrueFromStart() {
        tracker.addEntry(true);
        tracker.addEntry(true);
        tracker.addEntry(false);
        assertThat(tracker.getConsecutive(true)).isEqualTo(2);
        assertThat(tracker.getConsecutive(false)).isZero();
    }

    @Test
    void consecutiveFalseFromStart() {
        tracker.addEntry(false);
        tracker.addEntry(false);
        tracker.addEntry(true);
        assertThat(tracker.getConsecutive(false)).isEqualTo(2);
    }

    @Test
    void toStringNotEmpty() {
        tracker.addEntry(true);
        tracker.addEntry(false);
        String s = tracker.toString();
        assertThat(s).startsWith("[").endsWith("]");
        assertThat(s).contains("true").contains("false");
    }

    @Test
    void toStringWhenFull() {
        for (int i = 0; i < 5; i++) {
            tracker.addEntry(i % 2 == 0);
        }
        String s = tracker.toString();
        assertThat(s).startsWith("[").endsWith("]");
    }

    @Test
    void getCountReturnsBufferLength() {
        assertThat(tracker.getCount()).isEqualTo(5);
    }

    @Test
    void singleElementTracker() {
        BooleanTracker tiny = new BooleanTracker(1, 0);
        assertThat(tiny.isReady()).isFalse(); // next_ (0) is not > min_ (0)
        tiny.addEntry(true);
        // After adding 1 entry to size-1 buffer, next_ wraps to 0, full_ = true
        assertThat(tiny.isFull()).isTrue();
        assertThat(tiny.getPercentTrue(0.0f)).isCloseTo(1.0f, within(0.001f));
    }
}
