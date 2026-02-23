/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
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

import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameEventLogTest {

    @BeforeEach
    void clear() {
        GameEventLog.clear();
    }

    @Test
    void empty_returnsEmptyList() {
        assertThat(GameEventLog.getEntries()).isEmpty();
    }

    @Test
    void log_addsEntry() {
        GameEventLog.log("NEW_HAND", 1);
        List<GameEventLog.Entry> entries = GameEventLog.getEntries();
        assertThat(entries).hasSize(1);
        GameEventLog.Entry e = entries.get(0);
        assertThat(e.type()).isEqualTo("NEW_HAND");
        assertThat(e.tableId()).isEqualTo(1);
        assertThat(e.millis()).isPositive();
    }

    @Test
    void entries_returnedOldestFirst() {
        GameEventLog.log("NEW_HAND", 1);
        GameEventLog.log("CURRENT_PLAYER_CHANGED", 1);
        List<GameEventLog.Entry> entries = GameEventLog.getEntries();
        assertThat(entries.get(0).type()).isEqualTo("NEW_HAND");
        assertThat(entries.get(1).type()).isEqualTo("CURRENT_PLAYER_CHANGED");
    }

    @Test
    void clear_removesAllEntries() {
        GameEventLog.log("NEW_HAND", 1);
        GameEventLog.clear();
        assertThat(GameEventLog.getEntries()).isEmpty();
    }

    @Test
    void ringBuffer_evictsOldestWhenFull() {
        for (int i = 0; i <= GameEventLog.CAPACITY; i++) {
            GameEventLog.log("EVENT_" + i, 1);
        }
        List<GameEventLog.Entry> entries = GameEventLog.getEntries();
        assertThat(entries).hasSize(GameEventLog.CAPACITY);
        assertThat(entries.get(0).type()).isEqualTo("EVENT_1");
        assertThat(entries.get(entries.size() - 1).type()).isEqualTo("EVENT_" + GameEventLog.CAPACITY);
    }

    @Test
    void getLastN_returnsCorrectSubset() {
        for (int i = 0; i < 10; i++) {
            GameEventLog.log("EVENT_" + i, 1);
        }
        List<GameEventLog.Entry> last3 = GameEventLog.getLastN(3);
        assertThat(last3).hasSize(3);
        assertThat(last3.get(0).type()).isEqualTo("EVENT_7");
        assertThat(last3.get(2).type()).isEqualTo("EVENT_9");
    }

    @Test
    void getLastN_whenFewerThanNEntries_returnsAll() {
        GameEventLog.log("EVENT_0", 1);
        GameEventLog.log("EVENT_1", 1);
        assertThat(GameEventLog.getLastN(10)).hasSize(2);
    }

    @Test
    void getLastN_zero_returnsEmpty() {
        GameEventLog.log("EVENT_0", 1);
        assertThat(GameEventLog.getLastN(0)).isEmpty();
    }
}
