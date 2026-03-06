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
package com.donohoedigital.games.poker.protocol.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

class TournamentHistoryDataTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void jsonRoundTrip() throws Exception {
        TournamentHistoryData original = new TournamentHistoryData(100L, 3, "Friday Night Poker", "ONLINE",
                Instant.parse("2026-03-01T20:00:00Z"), Instant.parse("2026-03-01T23:30:00Z"), 1000, 500, 200, 5000, 45,
                0, true, 0);

        String json = MAPPER.writeValueAsString(original);
        TournamentHistoryData deserialized = MAPPER.readValue(json, TournamentHistoryData.class);

        assertEquals(original, deserialized);
    }

    @Test
    void jsonRoundTripInProgress() throws Exception {
        TournamentHistoryData original = new TournamentHistoryData(200L, 5, "Saturday Game", "PRACTICE",
                Instant.parse("2026-03-02T18:00:00Z"), null, 500, 0, 0, 0, 20, 12, false, 8500);

        String json = MAPPER.writeValueAsString(original);
        TournamentHistoryData deserialized = MAPPER.readValue(json, TournamentHistoryData.class);

        assertEquals(original, deserialized);
    }

    @Test
    void nullFieldsOmittedFromJson() throws Exception {
        TournamentHistoryData data = new TournamentHistoryData(null, 1, "Test", null, null, null, 100, 0, 0, 500, 10, 0,
                true, 0);

        String json = MAPPER.writeValueAsString(data);

        assertFalse(json.contains("\"gameId\""), "null gameId should be omitted");
        assertFalse(json.contains("\"tournamentType\""), "null tournamentType should be omitted");
        assertFalse(json.contains("\"startDate\""), "null startDate should be omitted");
        assertFalse(json.contains("\"endDate\""), "null endDate should be omitted");
    }

    @Test
    void totalSpentCalculation() {
        TournamentHistoryData data = new TournamentHistoryData(1L, 1, "Test", "ONLINE", Instant.now(), Instant.now(),
                1000, 500, 200, 5000, 10, 0, true, 0);

        assertEquals(1700, data.totalSpent());
    }

    @Test
    void totalSpentNoRebuysOrAddons() {
        TournamentHistoryData data = new TournamentHistoryData(1L, 1, "Test", "ONLINE", Instant.now(), Instant.now(),
                1000, 0, 0, 2000, 10, 0, true, 0);

        assertEquals(1000, data.totalSpent());
    }

    @Test
    void netPositive() {
        TournamentHistoryData data = new TournamentHistoryData(1L, 1, "Test", "ONLINE", Instant.now(), Instant.now(),
                1000, 500, 200, 5000, 10, 0, true, 0);

        assertEquals(3300, data.net());
    }

    @Test
    void netNegative() {
        TournamentHistoryData data = new TournamentHistoryData(1L, 8, "Test", "ONLINE", Instant.now(), Instant.now(),
                1000, 500, 200, 0, 10, 0, true, 0);

        assertEquals(-1700, data.net());
    }

    @Test
    void netBreakEven() {
        TournamentHistoryData data = new TournamentHistoryData(1L, 3, "Test", "ONLINE", Instant.now(), Instant.now(),
                1000, 0, 0, 1000, 10, 0, true, 0);

        assertEquals(0, data.net());
    }
}
