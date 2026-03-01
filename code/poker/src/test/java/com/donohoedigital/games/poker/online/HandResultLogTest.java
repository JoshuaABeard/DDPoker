/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.online.HandResultLog.HandResult;
import com.donohoedigital.games.poker.online.HandResultLog.PayoutDelta;
import com.donohoedigital.games.poker.online.HandResultLog.PotBreakdown;
import com.donohoedigital.games.poker.online.HandResultLog.PotWinnerAward;
import com.donohoedigital.games.poker.online.HandResultLog.WinnerResult;

/**
 * Tests for {@link HandResultLog} and its nested record types.
 */
class HandResultLogTest {

    @BeforeEach
    void clear() {
        HandResultLog.clear();
    }

    // =========================================================================
    // Nested record tests
    // =========================================================================

    @Test
    void potWinnerAward_holdsAllFields() {
        PotWinnerAward award = new PotWinnerAward(42L, 3, "Alice", 500);

        assertThat(award.playerId()).isEqualTo(42L);
        assertThat(award.seat()).isEqualTo(3);
        assertThat(award.name()).isEqualTo("Alice");
        assertThat(award.amount()).isEqualTo(500);
    }

    @Test
    void potBreakdown_holdsAllFields_andDefensivelyCopiesWinners() {
        PotWinnerAward award = new PotWinnerAward(1L, 0, "Bob", 200);
        PotBreakdown breakdown = new PotBreakdown(0, 200, List.of(award));

        assertThat(breakdown.potIndex()).isEqualTo(0);
        assertThat(breakdown.totalAmount()).isEqualTo(200);
        assertThat(breakdown.winners()).hasSize(1);
        assertThat(breakdown.winners().get(0).name()).isEqualTo("Bob");
    }

    @Test
    void winnerResult_holdsAllFields_andDefensivelyCopiesCards() {
        WinnerResult winner = new WinnerResult(7L, 2, "Carol", "Flush", "Ace-high flush", 300,
                List.of("As", "Ks", "Qs", "Js", "Ts"));

        assertThat(winner.playerId()).isEqualTo(7L);
        assertThat(winner.seat()).isEqualTo(2);
        assertThat(winner.name()).isEqualTo("Carol");
        assertThat(winner.handClass()).isEqualTo("Flush");
        assertThat(winner.handDescription()).isEqualTo("Ace-high flush");
        assertThat(winner.amountWon()).isEqualTo(300);
        assertThat(winner.cards()).containsExactly("As", "Ks", "Qs", "Js", "Ts");
    }

    @Test
    void payoutDelta_holdsAllFields() {
        PayoutDelta delta = new PayoutDelta(5L, 1, "Dave", true, 1000, 1500, 500);

        assertThat(delta.playerId()).isEqualTo(5L);
        assertThat(delta.seat()).isEqualTo(1);
        assertThat(delta.name()).isEqualTo("Dave");
        assertThat(delta.isHuman()).isTrue();
        assertThat(delta.startChips()).isEqualTo(1000);
        assertThat(delta.endChips()).isEqualTo(1500);
        assertThat(delta.delta()).isEqualTo(500);
    }

    @Test
    void handResult_holdsAllFields_andDefensivelyCopiesLists() {
        PotWinnerAward award = new PotWinnerAward(1L, 0, "Eve", 400);
        PotBreakdown pot = new PotBreakdown(0, 400, List.of(award));
        WinnerResult winner = new WinnerResult(1L, 0, "Eve", "Pair", "Pair of aces", 400, List.of("Ah", "Ad"));
        PayoutDelta delta = new PayoutDelta(1L, 0, "Eve", false, 800, 1200, 400);

        HandResult result = new HandResult(123456789L, 1, 5, List.of("Ah", "Kh", "Qh"), List.of(winner), List.of(pot),
                List.of(delta));

        assertThat(result.timestampMs()).isEqualTo(123456789L);
        assertThat(result.tableId()).isEqualTo(1);
        assertThat(result.handNumber()).isEqualTo(5);
        assertThat(result.communityCards()).containsExactly("Ah", "Kh", "Qh");
        assertThat(result.winners()).hasSize(1);
        assertThat(result.potBreakdown()).hasSize(1);
        assertThat(result.payoutDeltas()).hasSize(1);
    }

    // =========================================================================
    // HandResultLog static methods
    // =========================================================================

    @Test
    void getEntries_emptyInitially() {
        assertThat(HandResultLog.getEntries()).isEmpty();
    }

    @Test
    void getLatest_nullWhenEmpty() {
        assertThat(HandResultLog.getLatest()).isNull();
    }

    @Test
    void getForTable_nullWhenNoEntries() {
        assertThat(HandResultLog.getForTable(1)).isNull();
    }

    @Test
    void record_addsEntry() {
        HandResult result = buildHandResult(1, 1);
        HandResultLog.record(result);

        assertThat(HandResultLog.getEntries()).hasSize(1);
        assertThat(HandResultLog.getLatest()).isEqualTo(result);
    }

    @Test
    void record_nullIsIgnored() {
        HandResultLog.record(null);
        assertThat(HandResultLog.getEntries()).isEmpty();
    }

    @Test
    void getForTable_returnsLatestForTable() {
        HandResult result1 = buildHandResult(1, 1);
        HandResult result2 = buildHandResult(1, 2);
        HandResult result3 = buildHandResult(2, 3);

        HandResultLog.record(result1);
        HandResultLog.record(result2);
        HandResultLog.record(result3);

        assertThat(HandResultLog.getForTable(1)).isEqualTo(result2);
        assertThat(HandResultLog.getForTable(2)).isEqualTo(result3);
        assertThat(HandResultLog.getForTable(99)).isNull();
    }

    @Test
    void getLatest_returnsLastRecorded() {
        HandResult first = buildHandResult(1, 1);
        HandResult second = buildHandResult(1, 2);

        HandResultLog.record(first);
        HandResultLog.record(second);

        assertThat(HandResultLog.getLatest()).isEqualTo(second);
    }

    @Test
    void clear_removesAllEntries() {
        HandResultLog.record(buildHandResult(1, 1));
        HandResultLog.record(buildHandResult(2, 2));

        HandResultLog.clear();

        assertThat(HandResultLog.getEntries()).isEmpty();
        assertThat(HandResultLog.getLatest()).isNull();
        assertThat(HandResultLog.getForTable(1)).isNull();
        assertThat(HandResultLog.getForTable(2)).isNull();
    }

    @Test
    void ringBuffer_evictsOldestWhenAtCapacity() {
        for (int i = 1; i <= HandResultLog.CAPACITY + 1; i++) {
            HandResultLog.record(buildHandResult(i, i));
        }

        List<HandResult> entries = HandResultLog.getEntries();
        assertThat(entries).hasSize(HandResultLog.CAPACITY);
        assertThat(entries.get(0).handNumber()).isEqualTo(2); // oldest evicted
        assertThat(entries.get(entries.size() - 1).handNumber()).isEqualTo(HandResultLog.CAPACITY + 1);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static HandResult buildHandResult(int tableId, int handNumber) {
        return new HandResult(System.currentTimeMillis(), tableId, handNumber, List.of(), List.of(), List.of(),
                List.of());
    }
}
