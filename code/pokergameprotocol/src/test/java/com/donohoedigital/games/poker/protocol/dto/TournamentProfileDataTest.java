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
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

class TournamentProfileDataTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void jsonRoundTrip() throws Exception {
        List<BlindLevelData> blinds = List.of(new BlindLevelData(1, 25, 50, 0, 20, false, "NOLIMIT_HOLDEM"),
                new BlindLevelData(2, 50, 100, 10, 20, false, "NOLIMIT_HOLDEM"),
                new BlindLevelData(3, 0, 0, 0, 10, true, null));

        TournamentProfileData original = new TournamentProfileData("Friday Night Poker", "Weekly tournament",
                "Welcome ${name}!", 90, 90, 10, true, 1000, 5000, blinds, true, "NOLIMIT_HOLDEM", "TIME", 10, 20, true,
                1000, 5000, 5000, 3, 5, "LESS_THAN", true, 500, 2500, 6, "SPOTS", 10.0, 50000, 9,
                List.of(30.0, 20.0, 15.0), "AUTO", "PERCENT", 5.0, 0, true, 50, 30, 30, 25, 20, 15, 60, true, 25, true,
                10, true, true, 100, 4, true, true, 3, "STARTING", true, Instant.parse("2026-03-15T19:00:00Z"), 10,
                true, List.of("player1", "player2"), false);

        String json = MAPPER.writeValueAsString(original);
        TournamentProfileData deserialized = MAPPER.readValue(json, TournamentProfileData.class);

        assertEquals(original, deserialized);
    }

    @Test
    void jsonRoundTripMinimalProfile() throws Exception {
        TournamentProfileData original = new TournamentProfileData("Simple Game", "Basic", null, 10, 10, 10, true, 100,
                1000, List.of(new BlindLevelData(1, 10, 20, 0, 15, false, "NOLIMIT_HOLDEM")), false, "NOLIMIT_HOLDEM",
                "TIME", 10, 15, false, 0, 0, 0, 0, 0, null, false, 0, 0, 0, "SPOTS", 0.0, 0, 1, null, "AUTO", null, 0.0,
                0, false, 0, 30, 0, 0, 0, 0, 15, false, 0, false, 0, false, false, 0, 4, true, false, 0, null, false,
                null, 0, false, null, false);

        String json = MAPPER.writeValueAsString(original);
        TournamentProfileData deserialized = MAPPER.readValue(json, TournamentProfileData.class);

        assertEquals(original, deserialized);
    }

    @Test
    void nullFieldsOmittedFromJson() throws Exception {
        TournamentProfileData data = new TournamentProfileData("Test", "Desc", null, 10, 10, 10, true, 100, 1000, null,
                false, "NOLIMIT_HOLDEM", "TIME", 10, 15, false, 0, 0, 0, 0, 0, null, false, 0, 0, 0, "SPOTS", 0.0, 0, 1,
                null, null, null, 0.0, 0, false, 0, 30, 0, 0, 0, 0, 15, false, 0, false, 0, false, false, 0, 4, true,
                false, 0, null, false, null, 0, false, null, false);

        String json = MAPPER.writeValueAsString(data);

        assertFalse(json.contains("\"greeting\""), "null greeting should be omitted");
        assertFalse(json.contains("\"blindLevels\""), "null blindLevels should be omitted");
        assertFalse(json.contains("\"spotAllocations\""), "null spotAllocations should be omitted");
        assertFalse(json.contains("\"invitees\""), "null invitees should be omitted");
    }
}
