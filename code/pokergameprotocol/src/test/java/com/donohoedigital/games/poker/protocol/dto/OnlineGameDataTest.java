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

class OnlineGameDataTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void jsonRoundTrip() throws Exception {
        OnlineGameData original = new OnlineGameData(123L, "https://poker.example.com/games/123", "HostPlayer",
                "ONLINE", "Friday Night Poker", "SERVER", Instant.parse("2026-03-01T20:00:00Z"),
                Instant.parse("2026-03-01T23:30:00Z"));

        String json = MAPPER.writeValueAsString(original);
        OnlineGameData deserialized = MAPPER.readValue(json, OnlineGameData.class);

        assertEquals(original, deserialized);
    }

    @Test
    void jsonRoundTripRunningGame() throws Exception {
        OnlineGameData original = new OnlineGameData(456L, "https://poker.example.com/games/456", "AnotherHost",
                "ONLINE", "Saturday Tournament", "COMMUNITY", Instant.parse("2026-03-02T18:00:00Z"), null);

        String json = MAPPER.writeValueAsString(original);
        OnlineGameData deserialized = MAPPER.readValue(json, OnlineGameData.class);

        assertEquals(original, deserialized);
    }

    @Test
    void nullFieldsOmittedFromJson() throws Exception {
        OnlineGameData data = new OnlineGameData(1L, null, "Host", "ONLINE", "Test", "SERVER",
                Instant.parse("2026-01-01T00:00:00Z"), null);

        String json = MAPPER.writeValueAsString(data);

        assertFalse(json.contains("\"url\""), "null url should be omitted");
        assertFalse(json.contains("\"endDate\""), "null endDate should be omitted");
    }
}
