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

class WsMessageLogTest {

    @BeforeEach
    void clear() {
        WsMessageLog.clear();
    }

    @Test
    void empty_returnsEmptyList() {
        assertThat(WsMessageLog.getEntries()).isEmpty();
    }

    @Test
    void logOutbound_addsEntry() {
        WsMessageLog.logOutbound("PLAYER_ACTION", "CALL:0");
        List<WsMessageLog.Entry> entries = WsMessageLog.getEntries();
        assertThat(entries).hasSize(1);
        WsMessageLog.Entry e = entries.get(0);
        assertThat(e.direction()).isEqualTo("OUT");
        assertThat(e.type()).isEqualTo("PLAYER_ACTION");
        assertThat(e.payload()).isEqualTo("CALL:0");
        assertThat(e.millis()).isPositive();
    }

    @Test
    void logInbound_addsEntry() {
        WsMessageLog.logInbound("HAND_STARTED", "{\"handNum\":1}");
        List<WsMessageLog.Entry> entries = WsMessageLog.getEntries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).direction()).isEqualTo("IN");
        assertThat(entries.get(0).type()).isEqualTo("HAND_STARTED");
    }

    @Test
    void entries_returnedOldestFirst() {
        WsMessageLog.logOutbound("PLAYER_ACTION", "FOLD:0");
        WsMessageLog.logInbound("HAND_STARTED", "{}");
        List<WsMessageLog.Entry> entries = WsMessageLog.getEntries();
        assertThat(entries.get(0).direction()).isEqualTo("OUT");
        assertThat(entries.get(1).direction()).isEqualTo("IN");
    }

    @Test
    void clear_removesAllEntries() {
        WsMessageLog.logOutbound("PLAYER_ACTION", "FOLD:0");
        WsMessageLog.logInbound("HAND_STARTED", "{}");
        WsMessageLog.clear();
        assertThat(WsMessageLog.getEntries()).isEmpty();
    }

    @Test
    void ringBuffer_evictsOldestWhenFull() {
        // Fill to capacity + 1
        for (int i = 0; i <= WsMessageLog.CAPACITY; i++) {
            WsMessageLog.logOutbound("TYPE_" + i, "payload_" + i);
        }
        List<WsMessageLog.Entry> entries = WsMessageLog.getEntries();
        assertThat(entries).hasSize(WsMessageLog.CAPACITY);
        // First entry should be TYPE_1 (TYPE_0 was evicted)
        assertThat(entries.get(0).type()).isEqualTo("TYPE_1");
        assertThat(entries.get(entries.size() - 1).type()).isEqualTo("TYPE_" + WsMessageLog.CAPACITY);
    }

    @Test
    void longPayload_truncated() {
        String longPayload = "x".repeat(1000);
        WsMessageLog.logOutbound("TEST", longPayload);
        String stored = WsMessageLog.getEntries().get(0).payload();
        assertThat(stored.length()).isLessThan(1000);
        assertThat(stored).endsWith("…");
    }

    @Test
    void nullPayload_treatedAsEmpty() {
        WsMessageLog.logInbound("TYPE", null);
        assertThat(WsMessageLog.getEntries().get(0).payload()).isEmpty();
    }
}
